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
import android.content.Context;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.app.lockscreen.LockScreenActivity;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

final class LockServiceInteractorImpl implements LockServiceInteractor {

  private static final String[] NOTIFICATION_SHADE_CLASSES = {
      // Nexus 6 is a Frame Layout
      "android.widget.FrameLayout"
  };
  private static final String[] IMM_KEYBOARD_CLASSES = {
      // Nexus 6 Google Keyboard is a plain View
      "android.view.View"
  };

  @NonNull private final Context appContext;
  @NonNull private final KeyguardManager keyguard;
  @NonNull private final InputMethodManager inputMethodManager;

  @Inject public LockServiceInteractorImpl(final @NonNull Context context,
      @NonNull KeyguardManager keyguard, @NonNull InputMethodManager inputMethodManager) {
    this.inputMethodManager = inputMethodManager;
    this.appContext = context.getApplicationContext();
    this.keyguard = keyguard;
  }

  /**
   * Check if the screen is the notification shade
   *
   * The screen cannot be locked. If it is locked, this is the lock screen
   *
   * The notification shade has the android systemui package and
   * can be its own class or a FrameLayout
   */
  @CheckResult @Override public boolean isEventCausedByNotificationShade(
      @NonNull String packageName, @NonNull String className) {
    boolean isNotificationShadeClass = false;
    for (final String notificationShadeClass : NOTIFICATION_SHADE_CLASSES) {
      Timber.d("Check if class %s is a known notification shade class %s", className,
          notificationShadeClass);
      if (notificationShadeClass.equals(className)) {
        Timber.d("Class %s is a notification shade", className);
        isNotificationShadeClass = true;
        break;
      }
    }

    return !keyguard.inKeyguardRestrictedInputMode() && packageName.equals(
        ANDROID_SYSTEM_UI_PACKAGE) && isNotificationShadeClass;
  }

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

  /**
   * If the IMM is open and accepting text, there is a good chance then that the event is
   * actually brought by the Keyboard window opening.
   */
  @Override public boolean isWindowFromKeyboard(@NonNull String packageName,
      @NonNull String className) {
    boolean isIMMPackage = false;
    // We fetch this every time so that we can handle updates to the IMM list
    final List<InputMethodInfo> enabledInputMethods =
        inputMethodManager.getEnabledInputMethodList();
    for (final InputMethodInfo methodInfo : enabledInputMethods) {
      Timber.d("IMM package: %s, service %s", methodInfo.getPackageName(),
          methodInfo.getServiceName());
      if (methodInfo.getPackageName().equals(packageName)) {
        Timber.d("Package %s is IMM package", packageName);
        isIMMPackage = true;
        break;
      }
    }

    if (isIMMPackage) {
      boolean isIMMClass = false;
      for (final String immClass : IMM_KEYBOARD_CLASSES) {
        if (immClass.equals(className)) {
          Timber.d("Class %s is IMM Class", className);
          isIMMClass = true;
          break;
        }
      }

      if (isIMMClass) {
        final boolean accepting = inputMethodManager.isAcceptingText();
        Timber.d("IMM isAcceptingText: %s", accepting);
        return accepting;
      }
    }

    Timber.d("Window event is not from IMM");
    return false;
  }

  @Override
  public boolean isWindowFromLockScreen(@NonNull String packageName, @NonNull String className) {
    Timber.d("Window is lock screen: %s, %s", packageName, className);
    return packageName.equals(PadLock.class.getPackage().getName()) && className.equals(
        LockScreenActivity.class.getName());
  }

  @Override public boolean isLiveEvent(@NonNull String packageName, @NonNull String className) {
    return !packageName.isEmpty() && !className.isEmpty();
  }

  @NonNull @WorkerThread @CheckResult @Override
  public Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Query DB for entry with PN %s and AN %s", packageName, activityName);
    return PadLockEntry.queryWithPackageActivityName(appContext, packageName, activityName);
  }
}
