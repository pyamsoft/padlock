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

package com.pyamsoft.padlock.lock;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.lock.master.MasterPinInteractor;
import com.pyamsoft.padlock.model.Recheck;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockScreenEntryInteractor {

  @SuppressWarnings("WeakerAccess") static final int DEFAULT_MAX_FAIL_COUNT = 2;
  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;
  @SuppressWarnings("WeakerAccess") @NonNull final JobSchedulerCompat jobSchedulerCompat;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockDB padLockDB;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends IntentService>
      recheckServiceClass;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @NonNull private final MasterPinInteractor pinInteractor;
  @SuppressWarnings("WeakerAccess") int failCount;

  @Inject LockScreenEntryInteractor(@NonNull Context context,
      @NonNull final PadLockPreferences preferences, @NonNull JobSchedulerCompat jobSchedulerCompat,
      @NonNull final MasterPinInteractor masterPinInteractor, @NonNull PadLockDB padLockDB,
      @NonNull Class<? extends IntentService> recheckServiceClass) {
    this.preferences = preferences;
    this.appContext = context.getApplicationContext();
    this.jobSchedulerCompat = jobSchedulerCompat;
    this.padLockDB = padLockDB;
    this.pinInteractor = masterPinInteractor;
    this.recheckServiceClass = recheckServiceClass;
  }

  @CheckResult @NonNull
  public Observable<Boolean> submitPin(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, String currentAttempt) {
    return pinInteractor.getMasterPin().map(masterPin -> {
      Timber.d("Attempt unlock: %s %s", packageName, activityName);
      Timber.d("Check entry is not locked: %d", lockUntilTime);
      if (System.currentTimeMillis() < lockUntilTime) {
        Timber.e("Entry is still locked. Fail unlock");
        return null;
      }

      final String pin;
      if (lockCode == null) {
        Timber.d("No app specific code, use Master PIN");
        pin = masterPin;
      } else {
        Timber.d("App specific code present, compare attempt");
        pin = lockCode;
      }
      return pin;
    }).flatMap(nullablePin -> unlockEntry(currentAttempt, nullablePin));
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<Boolean> unlockEntry(
      @NonNull String attempt, @NonNull String pin) {
    return Observable.fromCallable(() -> pin)
        .filter(pin1 -> pin1 != null)
        .flatMap(pin1 -> LockInteractor.get().checkSubmissionAttempt(attempt, pin1));
  }

  @CheckResult @NonNull Observable<Boolean> postUnlock(@NonNull String packageName,
      @NonNull String activityName, @NonNull String realName, @Nullable String lockCode,
      boolean isSystem, boolean shouldExclude, long ignoreTime) {
    return Observable.defer(() -> {
      final long ignoreMinutesInMillis = ignoreTime * 60 * 1000;
      final Observable<Long> whitelistObservable;
      final Observable<Integer> ignoreObservable;
      final Observable<Integer> recheckObservable;

      if (shouldExclude) {
        whitelistObservable =
            whitelistEntry(packageName, activityName, realName, lockCode, isSystem);
        ignoreObservable = Observable.just(0);
        recheckObservable = Observable.just(0);
      } else {
        whitelistObservable = Observable.just(0L);
        ignoreObservable = ignoreEntryForTime(ignoreMinutesInMillis, packageName, activityName);
        recheckObservable = queueRecheckJob(packageName, activityName, ignoreMinutesInMillis);
      }

      return Observable.zip(ignoreObservable, recheckObservable, whitelistObservable,
          (ignore, recheck, whitelist) -> {
            Timber.d("Result of Whitelist: %d", whitelist);
            Timber.d("Result of Ignore: %d", ignore);
            Timber.d("Result of Recheck: %d", recheck);

            // KLUDGE Just return something valid for now
            return Boolean.TRUE;
          });
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<Long> whitelistEntry(
      @NonNull String packageName, @NonNull String activityName, @NonNull String realName,
      @Nullable String lockCode, boolean isSystem) {
    Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName);
    return padLockDB.insert(packageName, realName, lockCode, 0, 0, isSystem, true);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<Integer> queueRecheckJob(
      @NonNull String packageName, @NonNull String activityName, long recheckTime) {
    return Observable.fromCallable(() -> {
      // Cancel any old recheck job for the class, but not the package
      final Intent intent = new Intent(appContext, recheckServiceClass);
      intent.putExtra(Recheck.EXTRA_PACKAGE_NAME, packageName);
      intent.putExtra(Recheck.EXTRA_CLASS_NAME, activityName);
      jobSchedulerCompat.cancel(intent);

      // Queue up a new recheck job
      jobSchedulerCompat.set(intent, System.currentTimeMillis() + recheckTime);

      // KLUDGE Just return something valid for now
      return 1;
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<Integer> ignoreEntryForTime(
      long ignoreMinutesInMillis, @NonNull String packageName, @NonNull String activityName) {
    final long newIgnoreTime = System.currentTimeMillis() + ignoreMinutesInMillis;
    Timber.d("Ignore %s %s until %d (for %d)", packageName, activityName, newIgnoreTime,
        ignoreMinutesInMillis);

    // Add an extra second here to artificially de-bounce quick requests, like those commonly in multi window mode
    return padLockDB.updateIgnoreTime(newIgnoreTime + 1000L, packageName, activityName);
  }

  @CheckResult @NonNull
  public Observable<TimePair> incrementAndGetFailCount(String packageName, String activityName) {
    return Observable.fromCallable(() -> ++failCount)
        .filter(count -> count > DEFAULT_MAX_FAIL_COUNT)
        .flatMap(integer -> getTimeoutPeriodMinutesInMillis())
        .filter(timeoutPeriod -> timeoutPeriod > 0)
        .flatMap(
            timeOutMinutesInMillis -> lockEntry(timeOutMinutesInMillis, packageName, activityName));
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Observable<Long> getTimeoutPeriodMinutesInMillis() {
    return Observable.fromCallable(preferences::getTimeoutPeriod).map(period -> period * 60 * 1000);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<TimePair> lockEntry(
      long timeOutMinutesInMillis, @NonNull String packageName, @NonNull String activityName) {
    final long currentTime = System.currentTimeMillis();
    final long newLockUntilTime = currentTime + timeOutMinutesInMillis;
    Timber.d("Lock %s %s until %d (%d)", packageName, activityName, newLockUntilTime,
        timeOutMinutesInMillis);

    return padLockDB.updateLockTime(newLockUntilTime, packageName, activityName).map(integer -> {
      Timber.d("Update result: %s", integer);
      return new TimePair(currentTime, newLockUntilTime);
    });
  }

  public void resetFailCount() {
    Timber.d("Reset fail count to 0");
    failCount = 0;
  }

  @NonNull @CheckResult public Observable<String> getHint() {
    return pinInteractor.getHint().map(s -> s == null ? "" : s);
  }

  static class TimePair {
    public final long currentTime;
    public final long lockUntilTime;

    TimePair(long currentTime, long lockUntilTime) {
      this.currentTime = currentTime;
      this.lockUntilTime = lockUntilTime;
    }
  }
}
