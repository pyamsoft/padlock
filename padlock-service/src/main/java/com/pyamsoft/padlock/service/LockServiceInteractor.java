/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.service;

import android.app.Activity;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.padlock.base.preference.LockScreenPreferences;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton class LockServiceInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;
  @SuppressWarnings("WeakerAccess") @NonNull final LockScreenPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final JobSchedulerCompat jobSchedulerCompat;
  @SuppressWarnings("WeakerAccess") @NonNull final KeyguardManager keyguardManager;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends Activity> lockScreenActivity;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;
  @NonNull private final Class<? extends IntentService> recheckService;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull String lastPackageName = "";
  @SuppressWarnings("WeakerAccess") @NonNull String lastClassName = "";
  @SuppressWarnings("WeakerAccess") @NonNull String activePackageName = "";
  @SuppressWarnings("WeakerAccess") @NonNull String activeClassName = "";
  @SuppressWarnings("WeakerAccess") Map<String, Boolean> lockScreenPassed;

  @Inject LockServiceInteractor(@NonNull Context context,
      @NonNull LockScreenPreferences preferences, @NonNull JobSchedulerCompat jobSchedulerCompat,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockDB padLockDB,
      @NonNull @Named("lockscreen") Class<? extends Activity> lockScreenActivityClass,
      @NonNull @Named("recheck") Class<? extends IntentService> recheckServiceClass,
      @NonNull LockServiceStateInteractor stateInteractor) {
    this.appContext = context.getApplicationContext();
    this.jobSchedulerCompat = jobSchedulerCompat;
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
    this.preferences = preferences;
    this.keyguardManager = (KeyguardManager) appContext.getSystemService(Context.KEYGUARD_SERVICE);
    this.lockScreenActivity = lockScreenActivityClass;
    this.recheckService = recheckServiceClass;
    this.stateInteractor = stateInteractor;
    lockScreenPassed = new HashMap<>();
  }

  @SuppressWarnings("WeakerAccess") void reset() {
    Timber.i("Reset name state");
    lastPackageName = "";
    lastClassName = "";
    activeClassName = "";
    activePackageName = "";
    lockScreenPassed.clear();
  }

  /**
   * public
   */
  @CheckResult @NonNull Single<Boolean> processActiveIfMatching(@NonNull String packageName,
      @NonNull String className) {
    return Single.fromCallable(() -> {
      Timber.d("Check against current window values: %s, %s", activePackageName, activeClassName);
      // We can replace the actual passed classname with the stored classname because:
      // either it is equal to the passed name or the passed name is PACKAGE
      // which will respond to any class name
      return activePackageName.equals(packageName) && (activeClassName.equals(className)
          || className.equals(PadLockEntry.PACKAGE_ACTIVITY_NAME));
    });
  }

  /**
   * public
   */
  @CheckResult @NonNull Single<PadLockEntry> processEvent(@NonNull String packageName,
      @NonNull String className, @NonNull RecheckStatus forcedRecheck) {
    final Single<Boolean> windowEventObservable =
        stateInteractor.isServiceEnabled()
            .filter(enabled -> {
              if (!enabled) {
                Timber.e("Service is not user-enabled. Ignore");
                reset();
              }
              return enabled;
            })
            .flatMap(enabled -> isDeviceLocked().map(deviceLocked -> {
              if (deviceLocked) {
                Timber.w("Device is locked, reset lastPackage/lastClass");
                reset();
              }
              return enabled;
            }))
            .flatMap(enabled -> isEventFromActivity(packageName, className))
            .filter(fromActivity -> {
              if (!fromActivity) {
                Timber.w("Event is not caused by an Activity. P: %s, C: %s. Ignore", packageName,
                    className);
              }
              return fromActivity;
            })
            .flatMap(fromActivity -> isDeviceLocked().flatMap(deviceLocked -> {
              if (deviceLocked) {
                return isRestrictedWhileLocked();
              } else {
                return Maybe.just(Boolean.FALSE);
              }
            }))
            .filter(restrictedWhileLocked -> {
              if (restrictedWhileLocked) {
                Timber.w("Locking is restricted while device in keyguard: %s %s", packageName,
                    className);
                return Boolean.FALSE;
              } else {
                return Boolean.TRUE;
              }
            })
            .flatMap(notLocked -> isWindowFromLockScreen(packageName, className))
            .filter(isLockScreen -> {
              if (isLockScreen) {
                Timber.w("Event for package %s class: %s is caused by LockScreen. Ignore",
                    packageName, className);
              }
              return !isLockScreen;
            })
            .map(fromLockScreen -> {
              final boolean passed = !fromLockScreen;
              activePackageName = packageName;
              activeClassName = className;
              return passed;
            })
            .toSingle(Boolean.FALSE);

    return Single.zip(windowEventObservable, hasNameChanged(packageName, lastPackageName),
        hasNameChanged(className, lastClassName), isOnlyLockOnPackageChange(),
        (windowEventObserved, packageChanged, classChanged, lockOnPackageChange) -> {
          if (!windowEventObserved) {
            Timber.e("Failed to pass window checking");
            return Boolean.FALSE;
          }

          if (packageChanged) {
            Timber.d("Last Package: %s - New Package: %s", lastPackageName, packageName);
            lastPackageName = packageName;
          }
          if (classChanged) {
            Timber.d("Last Class: %s - New Class: %s", lastClassName, className);
            lastClassName = className;
          }

          boolean windowHasChanged = classChanged;
          if (lockOnPackageChange) {
            windowHasChanged &= packageChanged;
          }

          if (forcedRecheck == RecheckStatus.FORCE) {
            Timber.d("Pass filter via forced recheck");
            windowHasChanged = true;
          }

          Boolean lockPassed = lockScreenPassed.get(packageName + className);
          if (lockPassed == null) {
            Timber.w("No lock map entry exists for: %s, %s", packageName, className);
            Timber.w("default to False");
            lockPassed = false;
          }
          return windowHasChanged || !lockPassed;
        }).filter(lockApp -> lockApp).flatMap(aBoolean -> {
      Timber.d("Get list of locked classes with package: %s, class: %s", packageName, className);
      setLockScreenPassed(packageName, className, false);
      return getEntry(packageName, className);
    }).filter(padLockEntry -> !PadLockEntry.isEmpty(padLockEntry)).filter(entry -> {
      final long ignoreUntilTime = entry.ignoreUntilTime();
      final long currentTime = System.currentTimeMillis();
      Timber.d("Ignore until time: %d", ignoreUntilTime);
      Timber.d("Current time: %d", currentTime);
      return currentTime >= ignoreUntilTime;
    }).filter(entry -> {
      if (PadLockEntry.PACKAGE_ACTIVITY_NAME.equals(entry.activityName()) && entry.whitelist()) {
        throw new RuntimeException(
            "PACKAGE entry for package: " + entry.packageName() + " cannot be whitelisted");
      }

      Timber.d("Filter out if whitelisted packages");
      return !entry.whitelist();
    }).toSingle(PadLockEntry.EMPTY);
  }

  /**
   * public
   */
  void setLockScreenPassed(@NonNull String packageName, @NonNull String className, boolean b) {
    Timber.d("Set lockScreenPassed: %s, %s, [%s]", packageName, className, b);
    lockScreenPassed.put(packageName + className, b ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * Clean up the lock service, cancel background jobs
   *
   * public
   */
  void cleanup() {
    Timber.d("Cleanup LockService");
    final Intent intent = new Intent(appContext, recheckService);
    jobSchedulerCompat.cancel(intent);
  }

  /**
   * Return true if the window event is caused by an Activity
   */
  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<Boolean> isEventFromActivity(
      @NonNull String packageName, @NonNull String className) {
    Timber.d("Check event from activity: %s %s", packageName, className);
    return packageManagerWrapper.getActivityInfo(packageName, className)
        .map(activityInfo -> activityInfo != null);
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Single<Boolean> hasNameChanged(
      @NonNull String name, @NonNull String oldName) {
    return Single.fromCallable(() -> !name.equals(oldName));
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<Boolean> isWindowFromLockScreen(
      @NonNull String packageName, @NonNull String className) {
    return Maybe.fromCallable(() -> {
      final String lockScreenPackageName = appContext.getPackageName();
      final String lockScreenClassName = lockScreenActivity.getName();
      Timber.d("Check if window is lock screen (%s %s)", lockScreenPackageName,
          lockScreenClassName);

      final boolean isPackage = packageName.equals(lockScreenPackageName);
      return isPackage && className.equals(lockScreenClassName);
    });
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Single<Boolean> isOnlyLockOnPackageChange() {
    return Single.fromCallable(preferences::getLockOnPackageChange);
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<PadLockEntry> getEntry(
      @NonNull String packageName, @NonNull String activityName) {
    return padLockDB.queryWithPackageActivityNameDefault(packageName, activityName).toMaybe();
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<Boolean> isRestrictedWhileLocked() {
    return Maybe.fromCallable(preferences::isIgnoreInKeyguard);
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<Boolean> isDeviceLocked() {
    return Maybe.fromCallable(() -> keyguardManager.inKeyguardRestrictedInputMode()
        || keyguardManager.isKeyguardLocked());
  }
}
