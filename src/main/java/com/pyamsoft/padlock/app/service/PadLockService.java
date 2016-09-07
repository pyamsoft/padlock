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

package com.pyamsoft.padlock.app.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.accessibility.AccessibilityEvent;
import timber.log.Timber;

public class PadLockService extends AccessibilityService {

  private static volatile PadLockService instance = null;
  private ScreenOnOffReceiver screenOnOffReceiver;

  @NonNull @CheckResult private static synchronized PadLockService getInstance() {
    if (instance == null) {
      throw new NullPointerException("Current service instance is NULL");
    }
    return instance;
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess")
  static synchronized void setInstance(@Nullable PadLockService i) {
    instance = i;
  }

  @CheckResult public static boolean isRunning() {
    return instance != null;
  }

  public static void finish() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      getInstance().disableSelf();
    }
  }

  public static void passLockScreen() {
    getInstance().getScreenOnOffReceiver().passLockScreen();
  }

  public static void recheck(@NonNull String packageName, @NonNull String className) {
    if (!packageName.isEmpty() && !className.isEmpty()) {
      getInstance().getScreenOnOffReceiver().recheck(packageName, className);
    }
  }

  @Override public void onAccessibilityEvent(final @Nullable AccessibilityEvent event) {
    if (event == null) {
      Timber.e("AccessibilityEvent is NULL");
      return;
    }

    final CharSequence eventPackage = event.getPackageName();
    final CharSequence eventClass = event.getClassName();

    if (eventPackage != null && eventClass != null) {
      final String pName = eventPackage.toString();
      final String cName = eventClass.toString();
      if (!pName.isEmpty() && !cName.isEmpty()) {
        screenOnOffReceiver.processAccessibilityEvent(pName, cName, false);
      }
    } else {
      Timber.e("Missing needed data");
    }
  }

  @Override public void onInterrupt() {
    Timber.e("onInterrupt");
  }

  @Override public boolean onUnbind(Intent intent) {
    Timber.d("onUnbind");
    setInstance(null);
    screenOnOffReceiver.unregister();
    return super.onUnbind(intent);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    Timber.d("onDestroy");
    screenOnOffReceiver.destroy();
  }

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    Timber.d("onServiceConnected");
    if (screenOnOffReceiver != null) {
      screenOnOffReceiver.unregister();
      screenOnOffReceiver.destroy();
    }

    screenOnOffReceiver = new ScreenOnOffReceiver(getApplicationContext());
    screenOnOffReceiver.register();
    setInstance(this);
  }

  @CheckResult @NonNull ScreenOnOffReceiver getScreenOnOffReceiver() {
    if (screenOnOffReceiver == null) {
      throw new NullPointerException("ScreenOnOffReceiver is NULL");
    }
    return screenOnOffReceiver;
  }
}
