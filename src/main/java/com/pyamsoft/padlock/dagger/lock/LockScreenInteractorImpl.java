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
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.job.RecheckJob;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @NonNull private final MasterPinInteractor pinInteractor;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") int failCount;

  @Inject LockScreenInteractorImpl(final @NonNull Context context,
      @NonNull final PadLockPreferences preferences,
      @NonNull final MasterPinInteractor masterPinInteractor,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.pinInteractor = masterPinInteractor;
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Boolean> lockEntry(@NonNull String packageName, @NonNull String activityName) {
    return PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, activityName)
        .first()
        .flatMap(padLockEntry -> {
          final long timeOutMinutesInMillis = preferences.getTimeoutPeriod() * 60 * 1000;
          Timber.d("Lock %s %s for %d", padLockEntry.packageName(), padLockEntry.activityName(),
              timeOutMinutesInMillis);
          return updateEntry(padLockEntry, System.currentTimeMillis() + timeOutMinutesInMillis,
              padLockEntry.ignoreUntilTime(), false);
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

  @NonNull @Override
  public Observable<Boolean> postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem, boolean exclude,
      long ignoreTime) {
    return PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, activityName)
        .first()
        .flatMap(entry -> {
          Timber.d("Run finishing unlock hooks");
          final long ignoreMinutesInMillis = ignoreTime * 60 * 1000;
          final Observable<Long> whitelistObservable;
          if (exclude) {
            whitelistObservable =
                whitelistEntry(packageName, activityName, realName, lockCode, isSystem);
          } else {
            whitelistObservable = Observable.just(0L);
          }

          final Observable<Integer> ignoreObservable;
          if (ignoreTime != 0 && !exclude) {
            ignoreObservable = ignoreEntryForTime(entry, ignoreMinutesInMillis);
          } else {
            ignoreObservable = Observable.just(0);
          }

          final Observable<Integer> recheckObservable;
          if (ignoreTime != 0 && preferences.isRecheckEnabled() && !exclude) {
            recheckObservable = queueRecheckJob(entry, ignoreMinutesInMillis);
          } else {
            recheckObservable = Observable.just(0);
          }

          return Observable.zip(ignoreObservable, recheckObservable, whitelistObservable,
              (ignore, recheck, whitelist) -> {
                Timber.d("Result of Ignore: %d", ignore);
                Timber.d("Result of Recheck: %d", recheck);
                Timber.d("Result of Whitelist: %d", whitelist);

                // KLUDGE Just return something valid for now
                return true;
              });
        });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<Long> whitelistEntry(
      @NonNull String packageName, @NonNull String activityName, @NonNull String realName,
      @Nullable String lockCode, boolean isSystem) {
    Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName);
    return PadLockDB.with(appContext).insert(packageName, realName, lockCode, 0, 0, isSystem, true);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<Integer> queueRecheckJob(
      PadLockEntry entry, long recheckTime) {
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

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Observable<Integer> ignoreEntryForTime(
      @NonNull PadLockEntry oldValues, long ignoreMinutesInMillis) {
    Timber.d("Ignore %s %s for %d", oldValues.packageName(), oldValues.activityName(),
        ignoreMinutesInMillis);
    return updateEntry(oldValues, oldValues.lockUntilTime(),
        System.currentTimeMillis() + ignoreMinutesInMillis, oldValues.whitelist());
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Observable<Integer> updateEntry(
      @NonNull PadLockEntry values, long lockUntilTime, long ignoreUntilTime, boolean whitelist) {
    Timber.d("Update entry: %s, %s", values.packageName(), values.activityName());
    return PadLockDB.with(appContext)
        .updateWithPackageActivityName(values.packageName(), values.activityName(),
            values.lockCode(), lockUntilTime, ignoreUntilTime, values.systemApplication(),
            whitelist);
  }

  @NonNull @CheckResult @Override public Observable<Long> getDefaultIgnoreTime() {
    return Observable.defer(() -> Observable.just(preferences.getDefaultIgnoreTime()));
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<String> getDisplayName(@NonNull String packageName) {
    return packageManagerWrapper.loadPackageLabel(packageName);
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
