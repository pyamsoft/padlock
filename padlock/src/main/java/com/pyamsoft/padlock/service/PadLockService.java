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
import android.support.annotation.VisibleForTesting;
import android.view.accessibility.AccessibilityEvent;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.lock.LockScreenActivity;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.lang.ref.WeakReference;
import javax.inject.Inject;
import timber.log.Timber;

public class PadLockService extends AccessibilityService
    implements LockServicePresenter.LockService {

  private static volatile PadLockService instance = null;
  @Inject LockServicePresenter presenter;
  private Intent lockActivity;

  @NonNull @CheckResult private static synchronized PadLockService getInstance() {
    if (instance == null) {
      throw new NullPointerException("Current service instance is NULL");
    }

    //noinspection ConstantConditions
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
    getInstance().getPresenter().setLockScreenPassed();
  }

  public static void recheck(@NonNull String packageName, @NonNull String className) {
    if (!packageName.isEmpty() && !className.isEmpty()) {
      Timber.d("Recheck was requested for: %s, %s", packageName, className);
      getInstance().getPresenter().getActiveNames(packageName, className);
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
        presenter.processAccessibilityEvent(pName, cName,
            LockServicePresenter.RecheckStatus.NOT_FORCE);
      }
    } else {
      Timber.e("Missing needed data");
    }
  }

  @Override public void onInterrupt() {
    Timber.e("onInterrupt");
  }

  @Override public boolean onUnbind(Intent intent) {
    presenter.unbindView();
    setInstance(null);
    return super.onUnbind(intent);
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
  }

  @Override protected void onServiceConnected() {
    super.onServiceConnected();
    Timber.d("onServiceConnected");
    lockActivity = new Intent(getApplicationContext(), LockScreenActivity.class).setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

    Injector.get().provideComponent().plusLockServiceComponent().inject(this);
    presenter.bindView(this);
    setInstance(this);
  }

  @Override public void startLockScreen1(@NonNull PadLockEntry entry, @NonNull String realName) {
    craftIntent(lockActivity, entry, realName);
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName);

    final String packageName = entry.packageName();
    final String className = entry.activityName();
    final WeakReference<LockScreenActivity> isAlreadyLockedEntry =
        LockScreenActivity.hasLockedMapEntry(packageName, className);
    if (isAlreadyLockedEntry != null) {
      Timber.w("We have a locked entry for %s %s, attempt to finish it first", packageName,
          className);
      final LockScreenActivity oldLockScreen = isAlreadyLockedEntry.get();
      if (oldLockScreen != null) {
        Timber.w("HACK: Finish old lock screen for %s %s before staring new one", packageName,
            className);
        oldLockScreen.finish();
      }
    }

    getApplicationContext().startActivity(lockActivity);
  }

  @Override
  public void onActiveNamesRetrieved(@NonNull String packageName, @NonNull String activePackage,
      @NonNull String className, @NonNull String activeClass) {
    Timber.d("Check against current window values: %s, %s", activePackage, activeClass);
    if (activePackage.equals(packageName) && (activeClass.equals(className) || className.equals(
        PadLockEntry.PACKAGE_ACTIVITY_NAME))) {
      // We can replace the actual passed classname with the stored classname because:
      // either it is equal to the passed name or the passed name is PACKAGE
      // which will respond to any class name
      Timber.d("Run recheck for: %s %s", activePackage, activeClass);
      presenter.processAccessibilityEvent(activePackage, activeClass,
          LockServicePresenter.RecheckStatus.FORCE);
    }
  }
}
