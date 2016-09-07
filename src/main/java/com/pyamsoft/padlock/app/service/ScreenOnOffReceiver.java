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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.Singleton;
import com.pyamsoft.padlock.app.lock.LockScreenActivity;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.lock.LockScreenActivity2;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import timber.log.Timber;

public class ScreenOnOffReceiver extends BroadcastReceiver implements LockServicePresenter.LockService {

  @NonNull private final static IntentFilter SCREEN_FILTER;

  static {
    SCREEN_FILTER = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    SCREEN_FILTER.addAction(Intent.ACTION_SCREEN_ON);
  }

  @NonNull private final Context appContext;
  @NonNull private final Intent lockActivity;
  @NonNull private final Intent lockActivity2;
  @Inject LockServicePresenter presenter;
  private boolean isRegistered;
  private volatile boolean isScreenOff;

  ScreenOnOffReceiver(@NonNull Context context) {
    this.appContext = context.getApplicationContext();
    isRegistered = false;
    isScreenOff = false;

    lockActivity =
        new Intent(appContext, LockScreenActivity1.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    lockActivity2 =
        new Intent(appContext, LockScreenActivity2.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    Singleton.Dagger.with(appContext).plusLockService().inject(this);
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

  @Override public final void onReceive(final Context context, final Intent intent) {
    if (null != intent) {
      final String action = intent.getAction();
      switch (action) {
        case Intent.ACTION_SCREEN_OFF:
          Timber.d("Screen off event");
          isScreenOff = true;
          break;
        case Intent.ACTION_SCREEN_ON:
          Timber.d("Screen on event");
          isScreenOff = false;
          break;
        default:
          Timber.e("Invalid event: %s", action);
      }
    }
  }

  @Override public void startLockScreen1(@NonNull PadLockEntry entry, @NonNull String realName) {
    craftIntent(lockActivity2, lockActivity, entry, realName);
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName);
    appContext.startActivity(lockActivity);
  }

  @Override public void startLockScreen2(@NonNull PadLockEntry entry, @NonNull String realName) {
    craftIntent(lockActivity, lockActivity2, entry, realName);
    Timber.d("Start lock activity 2 for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName);
    appContext.startActivity(lockActivity2);
  }

  public void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      boolean forcedRecheck) {
    if (isScreenOff) {
      Timber.w("Not processing event while screen is off");
    } else {
      Timber.d("Process event: %s %s", packageName, className);
      presenter.processAccessibilityEvent(packageName, className, forcedRecheck);
    }
  }

  public void passLockScreen() {
    presenter.setLockScreenPassed();
  }

  public void recheck(@NonNull String packageName, @NonNull String className) {
    Timber.d("Recheck was requested for: %s, %s", packageName, className);
    final String servicePackage = presenter.getActivePackageName();
    final String serviceClass = presenter.getActiveClassName();
    Timber.d("Check against current window values: %s, %s", servicePackage, serviceClass);
    if (servicePackage.equals(packageName) && (serviceClass.equals(className) || className.equals(
        PadLockEntry.PACKAGE_ACTIVITY_NAME))) {
      // We can replace the actual passed classname with the stored classname because:
      // either it is equal to the passed name or the passed name is PACKAGE
      // which will respond to any class name
      Timber.d("Run recheck for: %s %s", servicePackage, serviceClass);
      processAccessibilityEvent(servicePackage, serviceClass, true);
    }
  }

  public void register() {
    if (!isRegistered) {
      create();
      appContext.registerReceiver(this, SCREEN_FILTER);
      isRegistered = true;
    }
  }

  private void create() {
    presenter.bindView(this);
  }

  public void unregister() {
    if (isRegistered) {
      appContext.unregisterReceiver(this);
      cleanup();
      isRegistered = false;
    }
  }

  public void destroy() {
    presenter.destroy();
  }

  private void cleanup() {
    presenter.unbindView();
  }
}

