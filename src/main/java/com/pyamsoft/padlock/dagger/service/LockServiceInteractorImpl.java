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
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.regex.Pattern;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

@SuppressWarnings("TryFinallyCanBeTryWithResources") final class LockServiceInteractorImpl
    implements LockServiceInteractor {

  private static final String[] UNLOCKED_CLASSES_REGEX = {
      // Don't allow locking of the PadLock
      LOCK_ACTIVITY_CLASS, CRASHLOG_ACTIVITY_CLASS,

      // Don't lock for Dialogs
      ANDROID_DIALOG_CLASS,

      // Don't allow locking for the Recent activities
      RECENTS_ACTIVITY_CLASS,

      // Don't change for just Views
      ANDROID_VIEW_CLASS_REGEX,
  };
  private static final String[] UNLOCKED_SUPERCLASS = {
      // Don't allow locking of the PadLock
      LOCK_ACTIVITY_CLASS, CRASHLOG_ACTIVITY_CLASS,

      // Don't lock for Dialogs
      ANDROID_DIALOG_CLASS,

      // Don't allow locking for the Recent activities
      RECENTS_ACTIVITY_CLASS,

      // Don't change for Views
      ANDROID_VIEW_CLASS,

      // Don't lock for the Permission request dialog
      ANDROID_PACKAGE_INSTALLER_PERMISSION_CLASS,

      // Dont lock for USB changer dialog
      ANDROID_SETTINGS_USB_CHOOSER_CLASS,
  };
  private static final String[] UNLOCKED_PACKAGES_REGEX = {
      // Don't allow locking for the system
      ANDROID_PACKAGE_REGEX,

      // Don't allow locking for the systemUI
      ANDROID_SYSTEM_UI_PACKAGE_REGEX,

      // Don't lock for the package installer
      ANDROID_PACKAGE_INSTALLER_REGEX,
  };

  @NonNull private final Context appContext;
  @NonNull private final KeyguardManager keyguard;

  @Inject public LockServiceInteractorImpl(final @NonNull Context context,
      @NonNull KeyguardManager keyguard) {
    this.appContext = context.getApplicationContext();
    this.keyguard = keyguard;
  }

  private static boolean isUnlocked(final String[] unlockedRegexArray, final String name) {
    boolean ret = false;
    for (final String anUnlockedRegexArray : unlockedRegexArray) {
      final Pattern pattern = Pattern.compile(anUnlockedRegexArray);
      if (pattern.matcher(name).matches()) {
        ret = true;
        break;
      }
    }
    return ret;
  }

  private static boolean isUnlockedTypeClass(final String className) {
    boolean ret = false;
    // Just compare classnames
    for (final String unlockedClass : UNLOCKED_SUPERCLASS) {
      ret = className.equalsIgnoreCase(unlockedClass);
      if (ret) {
        break;
      }
    }
    if (ret) {
      return true;
    }

    // Attempt to create actual class
    final Class<?> currentClass;
    try {
      currentClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
      Timber.e("Could not create class for: %s", className);
      return false;
    }

    // Compare actual class hierarchy to see if subclass
    for (final String unlockedClass : UNLOCKED_SUPERCLASS) {
      try {
        final Class<?> hardUnlocked = Class.forName(unlockedClass);
        Timber.d("Check if currentClass: %s is a subclass of %s", className, unlockedClass);
        ret = hardUnlocked.isAssignableFrom(currentClass);
        if (ret) {
          break;
        }
      } catch (ClassNotFoundException ignored) {
      }
    }
    return ret;
  }

  /**
   * Check if the screen is the notification shade
   *
   * The screen cannot be locked. If it is locked, this is the lock screen
   *
   * The notification shade has the android systemui package and
   * can be its own class or a FrameLayout
   */
  @Override public boolean isEventCausedByNotificationShade(String packageName, String className) {
    return !keyguard.inKeyguardRestrictedInputMode() &&
        packageName.equalsIgnoreCase(ANDROID_SYSTEM_UI_PACKAGE) &&
        className.equalsIgnoreCase(ANDROID_FRAME_LAYOUT_CLASS);
  }

  @Override public boolean isDeviceLocked() {
    boolean locked = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      locked = keyguard.isDeviceLocked();
    }
    return locked || keyguard.inKeyguardRestrictedInputMode();
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @Override public boolean hasNameChanged(String name, String oldName, String ignoreRegex) {
    final Pattern pattern = Pattern.compile(ignoreRegex);
    return !name.equalsIgnoreCase(oldName) && !pattern.matcher(name).matches();
  }

  /**
   * If we are coming from the LockActivity SELF, we just unlocked the screen. no need to
   * prompt again until we come from a different activity to the LockedApp
   */
  @Override public boolean isComingFromLockScreen(String oldClass) {
    return oldClass.equalsIgnoreCase(LOCK_ACTIVITY_CLASS);
  }

  /**
   * NOOP if the package current active is part of the user/defined whitelist
   */
  @Override public boolean isNameHardUnlocked(String packageName, String className) {
    return isUnlocked(UNLOCKED_CLASSES_REGEX, className) || isUnlocked(UNLOCKED_PACKAGES_REGEX,
        packageName) ||
        isUnlockedTypeClass(className);
  }

  @NonNull @WorkerThread @CheckResult @Override
  public Observable<PadLockEntry> getEntry(String packageName, String activityName) {
    return PadLockEntry.queryWithPackageActivityName(appContext, packageName, activityName);
  }
}
