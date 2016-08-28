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
import android.view.accessibility.AccessibilityEvent;
import com.pyamsoft.padlock.Singleton;
import com.pyamsoft.padlock.app.lock.LockScreenActivity;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.lock.LockScreenActivity2;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import timber.log.Timber;

public final class PadLockService extends AccessibilityService
    implements LockServicePresenter.LockService {

  private static volatile PadLockService instance = null;
  @Inject LockServicePresenter presenter;
  private Intent lockActivity;
  private Intent lockActivity2;

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

  public static void finish() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      getInstance().disableSelf();
    }
  }

  public static void passLockScreen() {
    final LockServicePresenter lockServicePresenter = getInstance().presenter;
    lockServicePresenter.setLockScreenPassed();
  }

  public static void recheck(@NonNull String packageName, @NonNull String className) {
    if (!packageName.isEmpty() && !className.isEmpty()) {
      final PadLockService service = getInstance();
      final LockServicePresenter presenter = service.presenter;
      Timber.d("Recheck was requested for: %s, %s", packageName, className);

      final String servicePackage = presenter.getActivePackageName();
      final String serviceClass = presenter.getActiveClassName();
      Timber.d("Check against current window values: %s, %s", servicePackage, serviceClass);
      if (servicePackage.equals(packageName) && (serviceClass.equals(className) || className.equals(
          PadLockEntry.PACKAGE_TAG))) {
        // We can replace the actual passed classname with the stored classname because:
        // either it is equal to the passed name or the passed name is PACKAGE
        // which will respond to any class name
        Timber.d("Run recheck for: %s %s", servicePackage, serviceClass);
        service.presenter.processAccessibilityEvent(servicePackage, serviceClass, true);
      }
    }
  }

  private static void craftIntent(@NonNull Intent removeIntent, @NonNull Intent addIntent,
      @NonNull PadLockEntry entry, @NonNull String realName) {
    removeIntent.removeExtra(LockScreenActivity.ENTRY_PACKAGE_NAME);
    removeIntent.removeExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME);
    removeIntent.removeExtra(LockScreenActivity.ENTRY_LOCK_CODE);
    removeIntent.removeExtra(LockScreenActivity.ENTRY_IS_SYSTEM);
    removeIntent.removeExtra(LockScreenActivity.ENTRY_REAL_NAME);
    addIntent.putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, entry.packageName());
    addIntent.putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, entry.activityName());
    addIntent.putExtra(LockScreenActivity.ENTRY_LOCK_CODE, entry.lockCode());
    addIntent.putExtra(LockScreenActivity.ENTRY_IS_SYSTEM,
        Boolean.toString(entry.systemApplication()));
    addIntent.putExtra(LockScreenActivity.ENTRY_REAL_NAME, realName);

    if (entry.whitelist()) {
      throw new RuntimeException("Cannot launch LockScreen for whitelisted applications");
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
        presenter.processAccessibilityEvent(pName, cName, false);
      }
    } else {
      Timber.e("Missing needed data");
    }
  }

  @Override public void onInterrupt() {
    Timber.e("onInterrupt");
  }

  @Override public void startLockScreen1(@NonNull PadLockEntry entry, @NonNull String realName) {
    craftIntent(lockActivity2, lockActivity, entry, realName);
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName);
    startActivity(lockActivity);
  }

  @Override public void startLockScreen2(@NonNull PadLockEntry entry, @NonNull String realName) {
    craftIntent(lockActivity, lockActivity2, entry, realName);
    Timber.d("Start lock activity 2 for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName);
    startActivity(lockActivity2);
  }

  @Override public boolean onUnbind(Intent intent) {
    Timber.d("onDestroy");
    presenter.unbindView();
    lockActivity = null;
    lockActivity2 = null;

    setInstance(null);
    return super.onUnbind(intent);
  }

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    Timber.d("onServiceConnected");
    lockActivity =
        new Intent(this, LockScreenActivity1.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    lockActivity2 =
        new Intent(this, LockScreenActivity2.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    Singleton.Dagger.with(this).plusLockService().inject(this);

    presenter.bindView(this);
    setInstance(this);
  }
}
