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

package com.pyamsoft.padlock.presenter.service;

import android.app.Activity;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.padlock.presenter.PadLockDB;
import com.pyamsoft.padlock.presenter.PadLockPreferences;
import com.pyamsoft.padlock.presenter.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.presenter.wrapper.PackageManagerWrapper;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockServiceInteractorImpl implements LockServiceInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final JobSchedulerCompat jobSchedulerCompat;
  @SuppressWarnings("WeakerAccess") @NonNull final KeyguardManager keyguardManager;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends Activity> lockScreenActivity;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;
  @NonNull private final Class<? extends IntentService> recheckService;

  @Inject LockServiceInteractorImpl(@NonNull Context context,
      @NonNull PadLockPreferences preferences, @NonNull JobSchedulerCompat jobSchedulerCompat,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockDB padLockDB,
      @NonNull Class<? extends Activity> lockScreenActivity,
      @NonNull Class<? extends IntentService> recheckService) {
    this.appContext = context.getApplicationContext();
    this.jobSchedulerCompat = jobSchedulerCompat;
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
    this.preferences = preferences;
    this.keyguardManager = (KeyguardManager) context.getApplicationContext()
        .getSystemService(Context.KEYGUARD_SERVICE);
    this.lockScreenActivity = lockScreenActivity;
    this.recheckService = recheckService;
  }

  /**
   * Clean up the lock service, cancel background jobs
   */
  @Override public void cleanup() {
    Timber.d("Cleanup LockService");
    Timber.d("Cancel ALL jobs");
    final Intent intent = new Intent(appContext, recheckService);
    jobSchedulerCompat.cancel(intent);
  }

  /**
   * Return true if the window event is caused by an Activity
   */
  @NonNull @Override public Observable<Boolean> isEventFromActivity(@NonNull String packageName,
      @NonNull String className) {
    Timber.d("Check event from activity: %s %s", packageName, className);
    return packageManagerWrapper.getActivityInfo(packageName, className)
        .map(activityInfo -> activityInfo != null);
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @NonNull @CheckResult @Override public Observable<Boolean> hasNameChanged(@NonNull String name,
      @NonNull String oldName) {
    return Observable.defer(() -> {
      Timber.d("Check if name has change");
      return Observable.just(!name.equals(oldName));
    });
  }

  @NonNull @Override public Observable<Boolean> isWindowFromLockScreen(@NonNull String packageName,
      @NonNull String className) {
    return Observable.defer(() -> {
      Timber.d("Check if window is from lock screen");
      final String lockScreenPackageName = lockScreenActivity.getPackage().getName();
      final String lockScreenClassName = lockScreenActivity.getName();

      final boolean isPackage = packageName.equals(lockScreenPackageName);
      final boolean lockScreen = isPackage && className.equals(lockScreenClassName);
      return Observable.just(lockScreen);
    });
  }

  @NonNull @Override public Observable<Boolean> isOnlyLockOnPackageChange() {
    return Observable.defer(() -> {
      Timber.d("Check if locking only happens on package change");
      return Observable.just(preferences.getLockOnPackageChange());
    });
  }

  @NonNull @CheckResult @Override
  public Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Query DB for entry with PN %s and AN %s", packageName, activityName);
    return padLockDB.queryWithPackageActivityNameDefault(packageName, activityName).first();
  }

  @NonNull @Override public Observable<Boolean> isRestrictedWhileLocked() {
    return Observable.defer(() -> {
      Timber.d("Check if window is restricted while device is locked");
      return Observable.just(preferences.isIgnoreInKeyguard());
    });
  }

  @NonNull @Override public Observable<Boolean> isDeviceLocked() {
    return Observable.defer(() -> {
      Timber.d("Check if device is locked");
      return Observable.just(
          keyguardManager.inKeyguardRestrictedInputMode() || keyguardManager.isKeyguardLocked());
    });
  }
}
