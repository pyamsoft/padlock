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

package com.pyamsoft.padlock.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import javax.inject.Inject;

class PadLockPreferences {

  @NonNull private static final String IS_SYSTEM = "is_system";
  @NonNull private static final String MASTER_PASSWORD = "master_password";
  @NonNull private static final String HINT = "hint";
  @NonNull private static final String AGREED = "agreed";
  @NonNull private static final String LOCK_LIST_ONBOARD = "list_onboard";
  @NonNull private static final String LOCK_DIALOG_ONBOARD = "dialog_onboard";
  @NonNull private final SharedPreferences preferences;
  @NonNull private final String ignoreTimeKey;
  @NonNull private final String ignoreTimeDefault;
  @NonNull private final String timeoutTimeKey;
  @NonNull private final String timeoutTimeDefault;
  @NonNull private final String lockPackageChangeKey;
  @NonNull private final String installListener;
  @NonNull private final String ignoreKeyguard;
  private final boolean lockPackageChangeDefault;
  private final boolean installListenerDefault;
  private final boolean ignoreKeyguardDefault;

  @Inject PadLockPreferences(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    final Resources res = appContext.getResources();
    preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    ignoreTimeKey = res.getString(R.string.ignore_time_key);
    ignoreTimeDefault = res.getString(R.string.ignore_time_default);
    timeoutTimeKey = res.getString(R.string.timeout_time_key);
    timeoutTimeDefault = res.getString(R.string.timeout_time_default);
    lockPackageChangeKey = res.getString(R.string.lock_package_change_key);
    lockPackageChangeDefault = res.getBoolean(R.bool.lock_package_change_default);
    installListener = res.getString(R.string.install_listener_key);
    installListenerDefault = res.getBoolean(R.bool.install_listener_default);
    ignoreKeyguard = res.getString(R.string.ignore_keyguard_key);
    ignoreKeyguardDefault = res.getBoolean(R.bool.ignore_keyguard_default);
  }

  @CheckResult public boolean isIgnoreInKeyguard() {
    return preferences.getBoolean(ignoreKeyguard, ignoreKeyguardDefault);
  }

  @CheckResult public boolean isInstallListenerEnabled() {
    return preferences.getBoolean(installListener, installListenerDefault);
  }

  @CheckResult public String getHint() {
    return preferences.getString(HINT, null);
  }

  public void setHint(@NonNull String hint) {
    preferences.edit().putString(HINT, hint).apply();
  }

  public void clearHint() {
    preferences.edit().remove(HINT).apply();
  }

  @CheckResult public boolean isDialogOnBoard() {
    return preferences.getBoolean(LOCK_DIALOG_ONBOARD, false);
  }

  @CheckResult public long getDefaultIgnoreTime() {
    return Long.parseLong(preferences.getString(ignoreTimeKey, ignoreTimeDefault));
  }

  @CheckResult public long getTimeoutPeriod() {
    return Long.parseLong(preferences.getString(timeoutTimeKey, timeoutTimeDefault));
  }

  @CheckResult public boolean getLockOnPackageChange() {
    return preferences.getBoolean(lockPackageChangeKey, lockPackageChangeDefault);
  }

  @CheckResult public boolean isSystemVisible() {
    return preferences.getBoolean(IS_SYSTEM, false);
  }

  public void setSystemVisible(boolean b) {
    preferences.edit().putBoolean(IS_SYSTEM, b).apply();
  }

  @CheckResult public String getMasterPassword() {
    return preferences.getString(MASTER_PASSWORD, null);
  }

  public void setMasterPassword(@NonNull String masterPassword) {
    preferences.edit().putString(MASTER_PASSWORD, masterPassword).apply();
  }

  public void clearMasterPassword() {
    preferences.edit().remove(MASTER_PASSWORD).apply();
  }

  @CheckResult public boolean hasAgreed() {
    return preferences.getBoolean(AGREED, false);
  }

  public void setAgreed() {
    preferences.edit().putBoolean(AGREED, true).apply();
  }

  @CheckResult public boolean isListOnBoard() {
    return preferences.getBoolean(LOCK_LIST_ONBOARD, false);
  }

  public void setListOnBoard() {
    preferences.edit().putBoolean(LOCK_LIST_ONBOARD, true).apply();
  }

  public void setInfoDialogOnBoard() {
    preferences.edit().putBoolean(LOCK_DIALOG_ONBOARD, true).apply();
  }

  public void clearAll() {
    preferences.edit().clear().apply();
  }
}
