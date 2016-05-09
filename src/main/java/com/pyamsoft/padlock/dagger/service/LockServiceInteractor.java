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

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.app.lockscreen.LockScreenActivity;
import com.pyamsoft.pydroid.crash.CrashLogActivity;
import com.squareup.sqlbrite.QueryObservable;

public interface LockServiceInteractor {

  String ANDROID_SYSTEM_UI_PACKAGE = "com.android.systemui";
  String ANDROID_PACKAGE = "android";

  String LOCK_ACTIVITY_CLASS = LockScreenActivity.class.getName();
  String CRASHLOG_ACTIVITY_CLASS = CrashLogActivity.class.getName();
  String ANDROID_FRAME_LAYOUT_CLASS = "android.widget.FrameLayout";
  String RECENTS_ACTIVITY_CLASS = "com.android.systemui.recents.RecentsActivity";
  String ANDROID_DIALOG_CLASS = "android.app.Dialog";
  String ANDROID_VIEW_CLASS = "android.view.View";
  String ANDROID_PACKAGE_INSTALLER_PERMISSION_CLASS =
      "com.android.packageinstaller.permission.ui.GrantPermissionsActivity";
  String ANDROID_SETTINGS_USB_CHOOSER_CLASS =
      "com.android.settings.deviceinfo.UsbModeChooserActivity";

  String GOOGLE_KEYBOARD_PACKAGE_REGEX = "^com.google.android.*inputmethod.*";
  String ANDROID_VIEW_CLASS_REGEX = "^android.view.*";
  String ANDROID_SYSTEM_UI_PACKAGE_REGEX = "^com.android.systemui";
  String ANDROID_PACKAGE_REGEX = "^android";
  String ANDROID_PACKAGE_INSTALLER_REGEX = "^com.android.packageinstaller";

  boolean isEventCausedByNotificationShade(String packageName, String className);

  boolean hasNameChanged(String name, String oldName, String ignoreRegex);

  boolean isComingFromLockScreen(String oldClass);

  boolean isNameHardUnlocked(String packageName, String className);

  @NonNull @WorkerThread QueryObservable getEntry(String packageName, String activityName);
}
