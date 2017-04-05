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

package com.pyamsoft.padlock.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.accessibility.AccessibilityEvent;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.padlock.lock.LockScreenActivity;
import com.pyamsoft.pydroid.bus.EventBus;
import javax.inject.Inject;
import timber.log.Timber;

public class PadLockService extends AccessibilityService
    implements LockServicePresenter.ProcessCallback {

  private static boolean running;
  @Inject LockServicePresenter presenter;
  private Intent lockActivity;

  @CheckResult public static boolean isRunning() {
    return running;
  }

  private static void setRunning(boolean running) {
    PadLockService.running = running;
  }

  public static void finish() {
    EventBus.get().publish(ServiceEvent.create(ServiceEvent.Type.FINISH));
  }

  public static void passLockScreen() {
    EventBus.get().publish(ServiceEvent.create(ServiceEvent.Type.PASS_LOCK));
  }

  public static void recheck(@NonNull String packageName, @NonNull String className) {
    if (!packageName.isEmpty() && !className.isEmpty()) {
      Timber.d("Recheck was requested for: %s, %s", packageName, className);
      EventBus.get().publish(RecheckEvent.create(packageName, className));
    }
  }

  private static void craftIntent(@NonNull Intent addIntent, @NonNull PadLockEntry entry,
      @NonNull String realName) {
    addIntent.putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, entry.packageName());
    addIntent.putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, entry.activityName());
    addIntent.putExtra(LockScreenActivity.ENTRY_LOCK_CODE, entry.lockCode());
    addIntent.putExtra(LockScreenActivity.ENTRY_IS_SYSTEM, entry.systemApplication());
    addIntent.putExtra(LockScreenActivity.ENTRY_REAL_NAME, realName);
    addIntent.putExtra(LockScreenActivity.ENTRY_LOCK_UNTIL_TIME, entry.lockUntilTime());

    if (entry.whitelist()) {
      throw new RuntimeException("Cannot launch LockScreen for whitelisted applications");
    }
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull LockServicePresenter getPresenter() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }

    return presenter;
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
        presenter.processAccessibilityEvent(pName, cName, RecheckStatus.NOT_FORCE, this);
      }
    } else {
      Timber.e("Missing needed data");
    }
  }

  @Override public void onInterrupt() {
    Timber.e("onInterrupt");
  }

  @Override public boolean onUnbind(Intent intent) {
    presenter.stop();
    setRunning(false);
    return super.onUnbind(intent);
  }

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    Timber.d("onServiceConnected");
    if (lockActivity == null) {
      lockActivity = new Intent(getApplicationContext(), LockScreenActivity.class).setFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    }

    if (presenter == null) {
      Injector.get().provideComponent().plusLockServiceComponent().inject(this);
    }

    LockServicePresenter.ProcessCallback callback = this;
    presenter.registerOnBus(new LockServicePresenter.ServiceCallback() {
      @Override public void onFinish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          disableSelf();
        }
      }

      @Override public void onPassLockScreen() {
        presenter.setLockScreenPassed();
      }

      @Override public void onRecheck(@NonNull String packageName, @NonNull String className) {
        presenter.processActiveApplicationIfMatching(packageName, className, callback);
      }
    });
    setRunning(true);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
  }

  @Override public void startLockScreen(@NonNull PadLockEntry entry, @NonNull String realName) {
    craftIntent(lockActivity, entry, realName);
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName);

    final String packageName = entry.packageName();
    final String className = entry.activityName();
    final LockScreenActivity isAlreadyLockedEntry =
        LockScreenActivity.hasLockedMapEntry(packageName, className);
    if (isAlreadyLockedEntry != null) {
      Timber.w("We have a locked entry for %s %s, attempt to finish it first", packageName,
          className);
      isAlreadyLockedEntry.finish();
    }

    getApplicationContext().startActivity(lockActivity);
  }
}
