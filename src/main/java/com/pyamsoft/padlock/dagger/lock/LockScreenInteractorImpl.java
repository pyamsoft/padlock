/*
 * Copyright 2016 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.dagger.lock;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.TagConstraint;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.dagger.db.DBInteractor;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

final class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @NonNull private final MasterPinInteractor pinInteractor;
  @NonNull private final DBInteractor dbInteractor;
  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferences preferences;
  @NonNull private final long[] ignoreTimes;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  private int failCount;

  @Inject public LockScreenInteractorImpl(final @NonNull Context context,
      @NonNull final PadLockPreferences preferences, @NonNull final DBInteractor dbInteractor,
      @NonNull final MasterPinInteractor masterPinInteractor,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.dbInteractor = dbInteractor;
    this.pinInteractor = masterPinInteractor;
    this.ignoreTimes = preferences.getIgnoreTimes();
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Boolean> lockEntry(@NonNull String packageName, @NonNull String activityName) {
    Timber.d("Lock entry: %s %s", packageName, activityName);
    return PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, activityName)
        .first()
        .map(padLockEntry -> {
          Timber.d("LOCKED entry, update entry in DB: ", padLockEntry);
          final long timeOutMinutesInMillis = preferences.getTimeoutPeriod() * 60 * 1000;
          final ContentValues contentValues = PadLockEntry.FACTORY.marshal()
              .packageName(padLockEntry.packageName())
              .activityName(padLockEntry.activityName())
              .lockCode(padLockEntry.lockCode())
              .lockUntilTime(System.currentTimeMillis() + timeOutMinutesInMillis)
              .ignoreUntilTime(padLockEntry.ignoreUntilTime())
              .systemApplication(padLockEntry.systemApplication())
              .asContentValues();
          return PadLockDB.with(appContext)
              .updateWithPackageActivityName(contentValues, padLockEntry.packageName(),
                  padLockEntry.activityName());
        })
        .map(integer -> {
          // TODO use result of update
          return true;
        });
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Boolean> unlockEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull String attempt) {
    Timber.d("Attempt unlock: %s %s", packageName, activityName);
    final Observable<PadLockEntry> dbObservable =
        PadLockDB.with(appContext).queryWithPackageActivityName(packageName, activityName).first();

    final Observable<String> masterPinObservable = pinInteractor.getMasterPin();
    return Observable.zip(dbObservable, masterPinObservable, (padLockEntry, masterPin) -> {
      final long lockUntilTime = padLockEntry.lockUntilTime();
      Timber.d("Check entry is not locked");
      if (System.currentTimeMillis() < lockUntilTime) {
        Timber.e("Entry is still locked. Fail unlock");
        return null;
      }

      if (PadLockEntry.isEmpty(padLockEntry)) {
        Timber.e("Entry is the EMPTY entry");
        return null;
      }

      final String appCode = padLockEntry.lockCode();
      String pin;
      if (appCode == null) {
        Timber.d("No app specific code, use Master PIN");
        pin = masterPin;
      } else {
        Timber.d("App specific code present, compare attempt");
        pin = appCode;
      }
      return pin;
    }).filter(pin -> pin != null).flatMap(pin -> checkSubmissionAttempt(attempt, pin));
  }

  @NonNull @Override
  public Observable<Boolean> postUnlock(@NonNull String packageName, @NonNull String activityName,
      boolean exclude, long ignoreTime) {
    Timber.d("Post unlock: %s %s", packageName, activityName);
    final Observable<PadLockEntry> dbObservable = PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, activityName)
        .first()
        .cache();
    return Observable.defer(() -> {
      Timber.d("Run finishing unlock hooks");
      return Observable.just(exclude);
    }).flatMap(exclude1 -> {
      if (exclude1) {
        Timber.d("EXCLUDE requested, delete entry from DB");
        return dbInteractor.deleteEntry(packageName, activityName);
      } else {
        Timber.d("EXCLUDE not requested");
        return Observable.just(1);
      }
    }).flatMap(deleteResult -> {
      // Ignore time is requested
      if (ignoreTime != 0) {
        Timber.d("Get entry to update");
        return dbObservable;
      } else {
        Timber.d("No update requested, empty entry");
        return Observable.just(PadLockEntry.empty());
      }
    }).flatMap(entry -> {
      if (!PadLockEntry.isEmpty(entry)) {
        final long ignoreMinutesInMillis = ignoreTime * 60 * 1000;
        Timber.d("Recheck requested, queue job");
        queueRecheckJob(entry, ignoreMinutesInMillis);

        Timber.d("IGNORE requested, update entry in DB");
        return ignoreEntryForTime(entry, ignoreMinutesInMillis);
      } else {
        Timber.d("IGNORE not requested");
        return Observable.just(1);
      }
    }).map(integer -> {
      // TODO do something with integer result
      Timber.d("Result for update: %d", integer);
      return true;
    });
  }

  // KLUDGE void, probably should return Observable to the stream
  @WorkerThread private void queueRecheckJob(PadLockEntry entry, long recheckTime) {
    // Cancel any old recheck job for the class, but not the package
    final String classTag = RecheckJob.CLASS_TAG_PREFIX + entry.activityName();
    Timber.d("Cancel jobs with class tag: %s", classTag);
    PadLock.getInstance().getJobManager().cancelJobs(TagConstraint.ANY, classTag);

    // Queue up a new recheck job
    final Job recheck = RecheckJob.create(entry.packageName(), entry.activityName(), recheckTime);
    PadLock.getInstance().getJobManager().addJob(recheck);
  }

  @NonNull @CheckResult
  private Observable<Integer> ignoreEntryForTime(@NonNull PadLockEntry oldValues,
      final long ignoreMinutesInMillis) {
    Timber.d("Ignore %s %s for %d", oldValues.packageName(), oldValues.activityName(),
        ignoreMinutesInMillis);
    final ContentValues contentValues = PadLockEntry.FACTORY.marshal()
        .packageName(oldValues.packageName())
        .activityName(oldValues.activityName())
        .lockCode(oldValues.lockCode())
        .lockUntilTime(oldValues.lockUntilTime())
        .ignoreUntilTime(System.currentTimeMillis() + ignoreMinutesInMillis)
        .systemApplication(oldValues.systemApplication())
        .asContentValues();
    return PadLockDB.with(appContext)
        .updateWithPackageActivityName(contentValues, oldValues.packageName(),
            oldValues.activityName());
  }

  @NonNull @CheckResult @Override public Observable<Long> getDefaultIgnoreTime() {
    return Observable.defer(() -> Observable.just(preferences.getDefaultIgnoreTime()));
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<String> getDisplayName(@NonNull String packageName) {
    return packageManagerWrapper.loadPackageLabel(packageName);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeForIndex(int index) {
    return Observable.defer(() -> Observable.just(ignoreTimes[index]));
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeOne() {
    return getIgnoreTimeForIndex(1);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeFive() {
    return getIgnoreTimeForIndex(2);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeTen() {
    return getIgnoreTimeForIndex(3);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeFifteen() {
    return getIgnoreTimeForIndex(4);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeTwenty() {
    return getIgnoreTimeForIndex(5);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeThirty() {
    return getIgnoreTimeForIndex(6);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeFourtyFive() {
    return getIgnoreTimeForIndex(7);
  }

  @NonNull @Override public Observable<Long> getIgnoreTimeSixty() {
    return getIgnoreTimeForIndex(8);
  }

  @NonNull @Override public Observable<Integer> incrementAndGetFailCount() {
    return Observable.defer(() -> Observable.just(++failCount));
  }

  @Override public void resetFailCount() {
    Timber.d("Reset fail count to 0");
    failCount = 0;
  }
}
