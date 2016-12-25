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

package com.pyamsoft.padlock.presenter.lock;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.model.Recheck;
import com.pyamsoft.padlock.presenter.PadLockDB;
import com.pyamsoft.padlock.presenter.PadLockPreferences;
import com.pyamsoft.padlock.presenter.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.presenter.wrapper.PackageManagerWrapper;
import javax.inject.Inject;
import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final JobSchedulerCompat jobSchedulerCompat;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockDB padLockDB;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends IntentService>
      recheckServiceClass;
  @NonNull private final MasterPinInteractor pinInteractor;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") int failCount;

  @Inject LockScreenInteractorImpl(@NonNull Context context,
      @NonNull final PadLockPreferences preferences, @NonNull JobSchedulerCompat jobSchedulerCompat,
      @NonNull final MasterPinInteractor masterPinInteractor,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockDB padLockDB,
      @NonNull Class<? extends IntentService> recheckServiceClass) {
    this.appContext = context.getApplicationContext();
    this.jobSchedulerCompat = jobSchedulerCompat;
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
    this.preferences = preferences;
    this.pinInteractor = masterPinInteractor;
    this.recheckServiceClass = recheckServiceClass;
  }

  @NonNull @Override
  public Observable<Long> lockEntry(long lockUntilTime, @NonNull String packageName,
      @NonNull String activityName) {
    return padLockDB.updateLockTime(lockUntilTime, packageName, activityName).map(integer -> {
      Timber.d("Update result: %s", integer);
      return lockUntilTime;
    });
  }

  @NonNull @Override public Observable<Long> getTimeoutPeriodMinutesInMillis() {
    return Observable.defer(() -> Observable.just(preferences.getTimeoutPeriod()))
        .map(period -> period * 60 * 1000);
  }

  @NonNull @Override public Observable<String> getMasterPin() {
    return pinInteractor.getMasterPin();
  }

  @NonNull @Override
  public Observable<Boolean> unlockEntry(@NonNull String attempt, @NonNull String pin) {
    return Observable.defer(() -> Observable.just(pin))
        .filter(pin1 -> pin1 != null)
        .flatMap(new Func1<String, Observable<Boolean>>() {
          @Override public Observable<Boolean> call(String pin) {
            return checkSubmissionAttempt(attempt, pin);
          }
        });
  }

  @Override @CheckResult @NonNull
  public Observable<Long> whitelistEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem) {
    Timber.d("Whitelist entry for %s %s (real %s)", packageName, activityName, realName);
    return padLockDB.insert(packageName, realName, lockCode, 0, 0, isSystem, true);
  }

  @Override @CheckResult @NonNull
  public Observable<Integer> queueRecheckJob(@NonNull String packageName,
      @NonNull String activityName, long recheckTime) {
    return Observable.defer(() -> Observable.just(preferences.isRecheckEnabled()))
        .map(recheckEnabled -> {
          if (!recheckEnabled) {
            Timber.e("Recheck is not enabled");
            return 0;
          }

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

  @NonNull @Override public Observable<Integer> ignoreEntryForTime(long ignoreMinutesInMillis,
      @NonNull String packageName, @NonNull String activityName) {
    final long newIgnoreTime = System.currentTimeMillis() + ignoreMinutesInMillis;
    Timber.d("Ignore %s %s until %d (for %d)", packageName, activityName, newIgnoreTime,
        ignoreMinutesInMillis);
    return padLockDB.updateIgnoreTime(newIgnoreTime, packageName, activityName);
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
