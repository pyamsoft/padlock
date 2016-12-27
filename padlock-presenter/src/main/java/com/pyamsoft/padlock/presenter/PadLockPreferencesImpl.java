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

package com.pyamsoft.padlock.presenter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.app.ApplicationPreferences;
import javax.inject.Inject;

class PadLockPreferencesImpl implements PadLockPreferences {

  @NonNull private static final String IS_SYSTEM = "is_system";
  @NonNull private static final String MASTER_PASSWORD = "master_password";
  @NonNull private static final String HINT = "hint";
  @NonNull private static final String AGREED = "agreed";
  @NonNull private static final String LOCK_LIST_ONBOARD = "list_onboard";
  @NonNull private static final String LOCK_DIALOG_ONBOARD = "dialog_onboard";
  @NonNull private final ApplicationPreferences preferences;
  @NonNull private final String ignoreTimeKey;
  @NonNull private final String ignoreTimeDefault;
  @NonNull private final String timeoutTimeKey;
  @NonNull private final String timeoutTimeDefault;
  @NonNull private final String lockPackageChangeKey;
  @NonNull private final String recheckKey;
  @NonNull private final String installListener;
  @NonNull private final String ignoreKeyguard;
  private final boolean lockPackageChangeDefault;
  private final boolean recheckDefault;
  private final boolean installListenerDefault;
  private final boolean ignoreKeyguardDefault;

  @Inject PadLockPreferencesImpl(final @NonNull Context context) {
    final Resources res = context.getApplicationContext().getResources();
    preferences = ApplicationPreferences.getInstance(context);
    ignoreTimeKey = res.getString(R.string.ignore_time_key);
    ignoreTimeDefault = res.getString(R.string.ignore_time_default);
    timeoutTimeKey = res.getString(R.string.timeout_time_key);
    timeoutTimeDefault = res.getString(R.string.timeout_time_default);
    lockPackageChangeKey = res.getString(R.string.lock_package_change_key);
    lockPackageChangeDefault = res.getBoolean(R.bool.lock_package_change_default);
    recheckKey = res.getString(R.string.recheck_key);
    recheckDefault = res.getBoolean(R.bool.recheck_default);
    installListener = res.getString(R.string.install_listener_key);
    installListenerDefault = res.getBoolean(R.bool.install_listener_default);
    ignoreKeyguard = res.getString(R.string.ignore_keyguard_key);
    ignoreKeyguardDefault = res.getBoolean(R.bool.ignore_keyguard_default);
  }

  @Override public boolean isIgnoreInKeyguard() {
    return preferences.get(ignoreKeyguard, ignoreKeyguardDefault);
  }

  @Override public boolean isInstallListenerEnabled() {
    return preferences.get(installListener, installListenerDefault);
  }

  @Override public String getHint() {
    return preferences.get(HINT, null);
  }

  @Override public void setHint(@NonNull String hint) {
    preferences.put(HINT, hint);
  }

  @Override public void clearHint() {
    preferences.remove(HINT);
  }

  @Override public boolean isLockInfoDialogOnBoard() {
    return preferences.get(LOCK_DIALOG_ONBOARD, false);
  }

  @Override public void setLockInfoDialogOnBoard() {
    preferences.put(LOCK_DIALOG_ONBOARD, true);
  }

  @Override public boolean isRecheckEnabled() {
    return preferences.get(recheckKey, recheckDefault);
  }

  @Override @CheckResult public final long getDefaultIgnoreTime() {
    return Long.parseLong(preferences.get(ignoreTimeKey, ignoreTimeDefault));
  }

  @Override @CheckResult public final long getTimeoutPeriod() {
    return Long.parseLong(preferences.get(timeoutTimeKey, timeoutTimeDefault));
  }

  @Override @CheckResult public final boolean getLockOnPackageChange() {
    return preferences.get(lockPackageChangeKey, lockPackageChangeDefault);
  }

  @Override @CheckResult public final boolean isSystemVisible() {
    return preferences.get(IS_SYSTEM, false);
  }

  @Override public final void setSystemVisible(final boolean b) {
    preferences.put(IS_SYSTEM, b);
  }

  @Override @CheckResult public final String getMasterPassword() {
    return preferences.get(MASTER_PASSWORD, null);
  }

  @Override public final void setMasterPassword(@NonNull String masterPassword) {
    preferences.put(MASTER_PASSWORD, masterPassword);
  }

  @Override public void clearMasterPassword() {
    preferences.remove(MASTER_PASSWORD);
  }

  @Override @CheckResult public boolean hasAgreed() {
    return preferences.get(AGREED, false);
  }

  @Override public void setAgreed() {
    preferences.put(AGREED, true);
  }

  @Override @CheckResult public boolean isOnBoard() {
    return preferences.get(LOCK_LIST_ONBOARD, false);
  }

  @Override public void setOnBoard() {
    preferences.put(LOCK_LIST_ONBOARD, true);
  }

  @Override public void clearAll() {
    preferences.clear(true);
  }
}
