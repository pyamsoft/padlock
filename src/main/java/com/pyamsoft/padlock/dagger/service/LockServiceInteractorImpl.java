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
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.app.lockscreen.LockScreenActivity;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

final class LockServiceInteractorImpl implements LockServiceInteractor {

  @NonNull private final Context appContext;
  @NonNull private final KeyguardManager keyguard;
  @NonNull private final PackageManager packageManager;

  @Inject public LockServiceInteractorImpl(final @NonNull Context context,
      @NonNull KeyguardManager keyguard, @NonNull PackageManager packageManager) {
    this.appContext = context.getApplicationContext();
    this.keyguard = keyguard;
    this.packageManager = packageManager;
  }

  /**
   * Return true if the window event is caused by an Activity
   */
  @Override public boolean isEventFromActivity(@NonNull String packageName,
      @NonNull String className) {
    if (packageName.isEmpty() || className.isEmpty()) {
      return false;
    }

    final ComponentName componentName = new ComponentName(packageName, className);
    try {
      final ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
      return activityInfo != null;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  /**
   * Return true if the device is currently locked
   */
  @CheckResult @Override public boolean isDeviceLocked() {
    boolean locked = false;
    Timber.d("Check if device is currently locked in some secure manner");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      Timber.d("Device has isDeviceLocked() call");
      locked = keyguard.isDeviceLocked();
    }
    return locked || keyguard.inKeyguardRestrictedInputMode();
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @CheckResult @Override public boolean hasNameChanged(@NonNull String name,
      @NonNull String oldName) {
    return !name.equals(oldName);
  }

  @Override
  public boolean isWindowFromLockScreen(@NonNull String packageName, @NonNull String className) {
    Timber.d("Window is lock screen: %s, %s", packageName, className);
    return packageName.equals(PadLock.class.getPackage().getName()) && className.equals(
        LockScreenActivity.class.getName());
  }

  @NonNull @WorkerThread @CheckResult @Override
  public Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Query DB for entry with PN %s and AN %s", packageName, activityName);
    return PadLockEntry.queryWithPackageActivityName(appContext, packageName, activityName);
  }
}
