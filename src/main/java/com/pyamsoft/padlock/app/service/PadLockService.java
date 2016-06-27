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
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.accessibility.AccessibilityEvent;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.app.lock.LockScreenActivity;
import com.pyamsoft.padlock.dagger.service.DaggerLockServiceComponent;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import timber.log.Timber;

public final class PadLockService extends AccessibilityService
    implements LockServicePresenter.LockService {

  private static volatile PadLockService instance = null;
  @Inject LockServicePresenter presenter;
  private Intent lockActivity = new Intent();

  @NonNull @CheckResult private static synchronized PadLockService getInstance() {
    if (instance == null) {
      throw new NullPointerException("Current service instance is NULL");
    }
    return instance;
  }

  private static synchronized void setInstance(@Nullable PadLockService i) {
    instance = i;
  }

  @CheckResult public static boolean isRunning() {
    return instance != null;
  }

  public static void passLockScreen() {
    final LockServicePresenter lockServicePresenter = getInstance().presenter;
    lockServicePresenter.setLockScreenPassed();
  }

  @Override public void onAccessibilityEvent(final @Nullable AccessibilityEvent event) {
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
    final String packageName = entry.packageName();
    final String activityName = entry.activityName();

    // Multiple task flag is needed to launch multiple lock screens on N
    lockActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    lockActivity.putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, packageName);
    lockActivity.putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, activityName);

    Timber.d("Start lock activity for entry: %s %s", packageName, activityName);
    startActivity(lockActivity);
  }

  @Override public boolean onUnbind(Intent intent) {
    Timber.d("onDestroy");
    presenter.unbindView();

    setInstance(null);
    return super.onUnbind(intent);
  }

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    Timber.d("onServiceConnected");
    lockActivity = new Intent(this, LockScreenActivity.class);
    DaggerLockServiceComponent.builder()
        .padLockComponent(PadLock.getInstance().getPadLockComponent())
        .build()
        .inject(this);

    presenter.bindView(this);
    setInstance(this);
  }
}
