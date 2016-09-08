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
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.job.RecheckJob;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final JobManager jobManager;
  @NonNull private final MasterPinInteractor pinInteractor;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") int failCount;

  @Inject LockScreenInteractorImpl(final @NonNull Context context,
      @NonNull final PadLockPreferences preferences, @NonNull JobManager jobManager,
      @NonNull final MasterPinInteractor masterPinInteractor,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.jobManager = jobManager;
    this.packageManagerWrapper = packageManagerWrapper;
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.pinInteractor = masterPinInteractor;
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Long> lockEntry(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long oldLockUntilTime, long ignoreUntilTime, boolean isSystem) {
    return Observable.defer(() -> Observable.just(preferences.getTimeoutPeriod()))
        .map(period -> period * 60 * 1000)
        .flatMap(timeOutMinutesInMillis -> {
          final long newLockUntilTime = System.currentTimeMillis() + timeOutMinutesInMillis;
          Timber.d("Lock %s %s until %d (%d)", packageName, activityName, newLockUntilTime,
              timeOutMinutesInMillis);
          return PadLockDB.with(appContext)
              .updateWithPackageActivityName(packageName, activityName, lockCode, newLockUntilTime,
                  ignoreUntilTime, isSystem, false)
              .map(integer -> {
                Timber.d("Update result: %s", integer);
                return newLockUntilTime;
              });
        });
  }

  @WorkerThread @NonNull @Override
  public Observable<Boolean> unlockEntry(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, @NonNull String attempt) {
    return pinInteractor.getMasterPin().map(masterPin -> {
      Timber.d("Attempt unlock: %s %s", packageName, activityName);
      Timber.d("Check entry is not locked: %d", lockUntilTime);
      if (System.currentTimeMillis() < lockUntilTime) {
        Timber.e("Entry is still locked. Fail unlock");
        return null;
      }

      String pin;
      if (lockCode == null) {
        Timber.d("No app specific code, use Master PIN");
        pin = masterPin;
      } else {
        Timber.d("App specific code present, compare attempt");
        pin = lockCode;
      }
      return pin;
    }).filter(pin -> pin != null).flatMap(pin -> checkSubmissionAttempt(attempt, pin));
  }

  @NonNull @Override
  public Observable<Boolean> postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, long lockUntilTime, boolean isSystem,
      boolean selectedExclude, long selectedIgnoreTime) {
    return Observable.defer(() -> {
      Timber.d("Run finishing unlock hooks");
      final long ignoreMinutesInMillis = selectedIgnoreTime * 60 * 1000;
      final Observable<Long> whitelistObservable;
      if (selectedExclude) {
        whitelistObservable =
            whitelistEntry(packageName, activityName, realName, lockCode, isSystem);
      } else {
        whitelistObservable = Observable.just(0L);
      }

      final Observable<Integer> ignoreObservable;
      if (selectedIgnoreTime != 0 && !selectedExclude) {
        ignoreObservable = ignoreEntryForTime(packageName, activityName, lockCode, lockUntilTime,
            ignoreMinutesInMillis, isSystem);
      } else {
        ignoreObservable = Observable.just(0);
      }

      final Observable<Integer> recheckObservable;
      if (selectedIgnoreTime != 0 && preferences.isRecheckEnabled() && !selectedExclude) {
        recheckObservable = queueRecheckJob(packageName, activityName, ignoreMinutesInMillis);
      } else {
        recheckObservable = Observable.just(0);
      }

      return Observable.zip(ignoreObservable, recheckObservable, whitelistObservable,
          (ignore, recheck, whitelist) -> {
            Timber.d("Result of Whitelist: %d", whitelist);
            Timber.d("Result of Ignore: %d", ignore);
            Timber.d("Result of Recheck: %d", recheck);

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
      @NonNull String packageName, @NonNull String activityName, long recheckTime) {
    return Observable.defer(() -> {
      // Cancel any old recheck job for the class, but not the package
      final String packageTag = RecheckJob.TAG_CLASS_PREFIX + packageName;
      final String classTag = RecheckJob.TAG_CLASS_PREFIX + activityName;
      Timber.d("Cancel jobs with package tag: %s and class tag: %s", packageTag, classTag);
      jobManager.cancelJobs(TagConstraint.ALL, packageTag, classTag);

      // Queue up a new recheck job
      final Job recheck = RecheckJob.create(packageName, activityName, recheckTime);
      jobManager.addJob(recheck);

      // KLUDGE Just return something valid for now
      return Observable.just(1);
    });
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Observable<Integer> ignoreEntryForTime(
      @NonNull String packageName, @NonNull String activityName, @Nullable String lockCode,
      long lockUntilTime, long ignoreMinutesInMillis, boolean isSystem) {
    final long newIgnoreTime = System.currentTimeMillis() + ignoreMinutesInMillis;
    Timber.d("Ignore %s %s until %d (for %d)", packageName, activityName, newIgnoreTime,
        ignoreMinutesInMillis);
    return PadLockDB.with(appContext)
        .updateWithPackageActivityName(packageName, activityName, lockCode, lockUntilTime,
            newIgnoreTime, isSystem, false);
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
