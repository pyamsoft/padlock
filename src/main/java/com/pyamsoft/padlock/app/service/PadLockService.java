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
import android.support.annotation.NonNull;
import android.view.accessibility.AccessibilityEvent;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.app.lockscreen.LockScreenActivity;
import com.pyamsoft.padlock.dagger.service.DaggerLockServiceComponent;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import timber.log.Timber;

public final class PadLockService extends AccessibilityService
    implements LockServicePresenter.LockService {

  private static volatile PadLockService instance = null;
  private static volatile boolean enabled = false;
  @Inject LockServicePresenter presenter;
  @NonNull private Intent lockActivity = new Intent();

  public static synchronized boolean isEnabled() {
    return enabled;
  }

  private static void setEnabled(boolean enabled) {
    PadLockService.enabled = enabled;
  }

  public static synchronized PadLockService getInstance() {
    return instance;
  }

  private static synchronized void setInstance(PadLockService i) {
    instance = i;
  }

  @Override public void onAccessibilityEvent(final AccessibilityEvent event) {
    if (event == null) {
      Timber.e("AccessibilityEvent is NULL");
      return;
    }

    final CharSequence eventPackage = event.getPackageName();
    final CharSequence eventClass = event.getClassName();
    if (eventPackage != null && eventClass != null) {
      presenter.processAccessibilityEvent(eventPackage.toString(), eventClass.toString());
    } else {
      Timber.e("Missing needed data");
      Timber.e("Package: %s", eventPackage);
      Timber.e("Class: %s", eventClass);
    }
  }

  @Override public void onInterrupt() {
    Timber.e("onInterrupt");
  }

  @Override public void startLockScreen(@NonNull PadLockEntry entry) {
    final long ignoreUntilTime = entry.ignoreUntilTime();
    final long currentTime = System.currentTimeMillis();
    if (currentTime < ignoreUntilTime) {
      Timber.i("Ignore period has not elapsed yet");
      Timber.i("Ignore until time: %d", ignoreUntilTime);
      Timber.i("Current time: %d", currentTime);
      return;
    }

    lockActivity.removeExtra(LockScreenActivity.ENTRY_PACKAGE_NAME);
    lockActivity.removeExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME);

    final String packageName = entry.packageName();
    final String activityName = entry.activityName();
    lockActivity.putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, packageName);
    lockActivity.putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, activityName);

    presenter.setLockScreenNotPassed();
    Timber.d("Start lock activity for entry: %s %s", packageName, activityName);
    startActivity(lockActivity);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    Timber.d("onDestroy");
    setEnabled(false);
    presenter.onDestroyView();
    setInstance(null);
  }

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    Timber.d("onServiceConnected");
    lockActivity = new Intent(this, LockScreenActivity.class);
    lockActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    DaggerLockServiceComponent.builder()
        .padLockComponent(PadLock.padLockComponent(this))
        .build()
        .inject(this);
    presenter.onCreateView(this);
    setEnabled(true);
    setInstance(this);
  }

  @Override public void passLockScreen() {
    presenter.setLockScreenPassed();
  }
}
