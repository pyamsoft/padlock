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

package com.pyamsoft.padlock.dagger.service;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.lock.LockScreenActivity;
import com.pyamsoft.padlock.app.sql.PadLockOpenHelper;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

final class LockServiceInteractorImpl implements LockServiceInteractor {

  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferences preferences;
  @NonNull private final KeyguardManager keyguard;
  @NonNull private final PackageManager packageManager;

  @Inject public LockServiceInteractorImpl(final @NonNull Context context,
      @NonNull PadLockPreferences preferences, @NonNull KeyguardManager keyguard,
      @NonNull PackageManager packageManager) {
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.keyguard = keyguard;
    this.packageManager = packageManager;
  }

  /**
   * Return true if the window event is caused by an Activity
   */
  @NonNull @Override public Observable<Boolean> isEventFromActivity(@NonNull String packageName,
      @NonNull String className) {
    return Observable.defer(() -> {
      if (packageName.isEmpty() || className.isEmpty()) {
        return Observable.just(false);
      }

      final ComponentName componentName = new ComponentName(packageName, className);
      try {
        final ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
        return Observable.just(activityInfo != null);
      } catch (PackageManager.NameNotFoundException e) {
        return Observable.just(false);
      }
    });
  }

  /**
   * Return true if the device is currently locked
   */
  @NonNull @CheckResult @Override public Observable<Boolean> isDeviceLocked() {
    return Observable.defer(() -> {
      boolean locked = false;
      Timber.d("Check if device is currently locked in some secure manner");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        Timber.d("Device has isDeviceLocked() call");
        locked = keyguard.isDeviceLocked();
      }
      return Observable.just(locked || keyguard.inKeyguardRestrictedInputMode());
    });
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @NonNull @CheckResult @Override public Observable<Boolean> hasNameChanged(@NonNull String name,
      @NonNull String oldName) {
    return Observable.defer(() -> Observable.just(!name.equals(oldName)));
  }

  @NonNull @Override public Observable<Boolean> isWindowFromLockScreen(@NonNull String packageName,
      @NonNull String className) {
    return Observable.defer(() -> Observable.just(
        packageName.equals(PadLock.class.getPackage().getName()) && className.equals(
            LockScreenActivity.class.getName())));
  }

  @NonNull @Override public Observable<Boolean> isOnlyLockOnPackageChange() {
    return Observable.defer(() -> Observable.just(preferences.getLockOnPackageChange()));
  }

  @NonNull @Override public Observable<Boolean> isLockWhenDeviceLocked() {
    return Observable.defer(() -> Observable.just(preferences.getLockOnDeviceLocked()));
  }

  @NonNull @CheckResult @Override
  public Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Query DB for entry with PN %s and AN %s", packageName, activityName);
    final Observable<PadLockEntry> specificActivityEntry =
        PadLockOpenHelper.queryWithPackageActivityName(appContext, packageName, activityName)
            .first();
    final Observable<PadLockEntry> packageActivityEntry =
        PadLockOpenHelper.queryWithPackageActivityName(appContext, packageName,
            PadLockEntry.PACKAGE_TAG).first();
    return Observable.zip(specificActivityEntry, packageActivityEntry,
        (specificEntry, packageEntry) -> {
          Timber.d("Check the specific entry for validity");
          if (!PadLockEntry.isEmpty(specificEntry)) {
            Timber.d("Specific entry PN %s, AN %s", specificEntry.packageName(),
                specificEntry.activityName());
            return specificEntry;
          }

          Timber.w("Specific entry is the EMPTY entry, try something else");
          if (!PadLockEntry.isEmpty(packageEntry)) {
            Timber.d("Package entry PN %s, AN %s", packageEntry.packageName(),
                packageEntry.activityName());
            return packageEntry;
          }

          Timber.w("Package entry is the EMPTY entry, return NULL");
          return null;
        }).filter(entry -> {
      final boolean filterOut = entry != null;
      Timber.d("Keep entry if not NULL: %s", filterOut);
      return filterOut;
    }).filter(entry -> {
      Timber.d("Check ignore time for: %s %s", entry.packageName(), entry.activityName());
      final long ignoreUntilTime = entry.ignoreUntilTime();
      final long currentTime = System.currentTimeMillis();
      if (currentTime < ignoreUntilTime) {
        Timber.d("Ignore period has not elapsed yet");
        Timber.d("Ignore until time: %d", ignoreUntilTime);
        Timber.d("Current time: %d", currentTime);
        return false;
      }

      Timber.d("Lock: %s %s", entry.packageName(), entry.activityName());
      return true;
    });
  }
}
