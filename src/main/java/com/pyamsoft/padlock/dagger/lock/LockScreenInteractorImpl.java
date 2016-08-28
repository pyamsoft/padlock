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

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.TagConstraint;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.Singleton;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @NonNull final MasterPinInteractor pinInteractor;
  @NonNull final Context appContext;
  @NonNull final PadLockPreferences preferences;
  @NonNull final long[] ignoreTimes;
  @NonNull final PackageManagerWrapper packageManagerWrapper;
  int failCount;

  @Inject LockScreenInteractorImpl(final @NonNull Context context,
      @NonNull final PadLockPreferences preferences,
      @NonNull final MasterPinInteractor masterPinInteractor,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.pinInteractor = masterPinInteractor;
    this.ignoreTimes = preferences.getIgnoreTimes();
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Boolean> lockEntry(@NonNull String packageName, @NonNull String activityName) {
    Timber.d("Lock entry: %s %s", packageName, activityName);
    return PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, activityName)
        .first()
        .flatMap(padLockEntry -> {
          final long timeOutMinutesInMillis = preferences.getTimeoutPeriod() * 60 * 1000;
          Timber.d("LOCKED entry, update entry in DB: %s", padLockEntry);
          Timber.d("lock for %d", timeOutMinutesInMillis);
          return PadLockDB.with(appContext)
              .updateWithPackageActivityName(padLockEntry.packageName(),
                  padLockEntry.activityName(), padLockEntry.lockCode(),
                  System.currentTimeMillis() + timeOutMinutesInMillis,
                  padLockEntry.ignoreUntilTime(), padLockEntry.systemApplication(), false);
        })
        .map(integer -> {
          Timber.d("Result of update: %d", integer);
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
      Timber.d("Check entry is not locked: %d", lockUntilTime);
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

  @CheckResult @NonNull private Observable<Boolean> isRecheckEnabled() {
    return Observable.defer(() -> Observable.just(preferences.isRecheckEnabled()));
  }

  @NonNull @Override
  public Observable<Boolean> postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem, boolean exclude,
      long ignoreTime) {
    Timber.d("Post unlock: %s %s", packageName, activityName);
    final long ignoreMinutesInMillis = ignoreTime * 60 * 1000;
    final Observable<PadLockEntry> dbObservable = PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, activityName)
        .first()
        .cache();
    return Observable.defer(() -> {
      Timber.d("Run finishing unlock hooks");
      return Observable.just(exclude);
    }).flatMap(exclude1 -> {
      if (exclude1) {
        Timber.d("EXCLUDE requested, whitelist entry in DB");
        return whitelistEntry(packageName, activityName, realName, lockCode, isSystem);
      } else {
        Timber.d("EXCLUDE not requested");
        return Observable.just(0L);
      }
    }).flatMap(whitelistResult -> {
      Timber.d("Whitelist result: %s", whitelistResult == 0L ? "NOTHING" : "SUCCESS");
      // TODO do something with long result
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
        Timber.d("IGNORE requested, update entry in DB");
        return ignoreEntryForTime(entry, ignoreMinutesInMillis);
      } else {
        Timber.d("IGNORE not requested");
        return Observable.just(0);
      }
    }).flatMap(ignoreResult -> {
      Timber.d("Ignore result: %s", ignoreResult == 0 ? "NOTHING" : "SUCCESS");
      // TODO do something with integer result
      return isRecheckEnabled();
    }).flatMap(recheckAllowed -> {
      if (ignoreTime != 0 && recheckAllowed) {
        Timber.d("Get entry to recheck");
        return dbObservable;
      } else {
        Timber.d("No update requested, empty entry");
        return Observable.just(PadLockEntry.empty());
      }
    }).flatMap(entry -> {
      if (!PadLockEntry.isEmpty(entry)) {
        Timber.d("RECHECK requested, queue new job");
        return queueRecheckJob(entry, ignoreMinutesInMillis);
      } else {
        Timber.d("RECHECK not requested");
        return Observable.just(0);
      }
    }).map(recheckResult -> {
      Timber.d("Recheck queued: %s", recheckResult == 0 ? "NO" : "YES");
      // TODO do something with integer result
      Timber.d("Result for recheck: %d", recheckResult);
      return true;
    });
  }

  @CheckResult @NonNull
  private Observable<Long> whitelistEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem) {
    Timber.d("Get entry for %s %s (real %s)", packageName, activityName, realName);
    return PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, realName)
        .first()
        .flatMap(padLockEntry -> {
          if (PadLockEntry.isEmpty(padLockEntry)) {
            Timber.d("No entry currently exists, create new one and whitelist it");
            return PadLockDB.with(appContext)
                .insert(packageName, realName, lockCode, 0, 0, isSystem, true);
          } else {
            Timber.d("Entry exists, update it");
            return PadLockDB.with(appContext)
                .updateWithPackageActivityName(padLockEntry.packageName(),
                    padLockEntry.activityName(), padLockEntry.lockCode(),
                    padLockEntry.lockUntilTime(), padLockEntry.ignoreUntilTime(),
                    padLockEntry.systemApplication(), true)
                .map(Integer::longValue);
          }
        });
  }

  // KLUDGE void, probably should return Observable to the stream
  @CheckResult @NonNull private Observable<Integer> queueRecheckJob(PadLockEntry entry,
      long recheckTime) {
    return Observable.defer(() -> {
      // Cancel any old recheck job for the class, but not the package
      final String packageTag = RecheckJob.TAG_CLASS_PREFIX + entry.packageName();
      final String classTag = RecheckJob.TAG_CLASS_PREFIX + entry.activityName();
      Timber.d("Cancel jobs with package tag: %s and class tag: %s", packageTag, classTag);
      Singleton.Jobs.with(appContext).cancelJobs(TagConstraint.ALL, packageTag, classTag);

      // Queue up a new recheck job
      final Job recheck = RecheckJob.create(entry.packageName(), entry.activityName(), recheckTime);
      Singleton.Jobs.with(appContext).addJob(recheck);

      // KLUDGE Just return something valid for now
      return Observable.just(1);
    });
  }

  @NonNull @CheckResult
  private Observable<Integer> ignoreEntryForTime(@NonNull PadLockEntry oldValues,
      final long ignoreMinutesInMillis) {
    Timber.d("Ignore %s %s for %d", oldValues.packageName(), oldValues.activityName(),
        ignoreMinutesInMillis);
    return PadLockDB.with(appContext)
        .updateWithPackageActivityName(oldValues.packageName(), oldValues.activityName(),
            oldValues.lockCode(), oldValues.lockUntilTime(),
            System.currentTimeMillis() + ignoreMinutesInMillis, oldValues.systemApplication(),
            oldValues.whitelist());
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

  @NonNull @Override public Observable<String> getHint() {
    return pinInteractor.getHint();
  }
}
