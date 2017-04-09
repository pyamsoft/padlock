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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import com.pyamsoft.padlock.base.preference.ClearPreferences;
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences;
import com.pyamsoft.padlock.base.preference.LockListPreferences;
import com.pyamsoft.padlock.base.preference.LockScreenPreferences;
import com.pyamsoft.padlock.base.preference.MasterPinPreferences;
import com.pyamsoft.padlock.base.preference.OnboardingPreferences;
import com.pyamsoft.padlock.model.LockScreenType;
import javax.inject.Inject;

class PadLockPreferencesImpl
    implements MasterPinPreferences, ClearPreferences, InstallListenerPreferences,
    LockListPreferences, LockScreenPreferences, OnboardingPreferences {

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
  @NonNull private final String lockScreenType;
  @NonNull private final String lockScreenTypeDefault;
  private final boolean lockPackageChangeDefault;
  private final boolean installListenerDefault;
  private final boolean ignoreKeyguardDefault;

  @Inject PadLockPreferencesImpl(@NonNull Context context) {
    Context appContext = context.getApplicationContext();
    Resources res = appContext.getResources();

    this.preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
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
    lockScreenType = res.getString(R.string.lock_screen_type_key);
    lockScreenTypeDefault = res.getString(R.string.lock_screen_type_default);
  }

  @NonNull @Override public LockScreenType getCurrentLockType() {
    return LockScreenType.valueOf(preferences.getString(lockScreenType, lockScreenTypeDefault));
  }

  @Override public boolean isIgnoreInKeyguard() {
    return preferences.getBoolean(ignoreKeyguard, ignoreKeyguardDefault);
  }

  @Override public boolean isInstallListenerEnabled() {
    return preferences.getBoolean(installListener, installListenerDefault);
  }

  @Nullable @Override public String getHint() {
    return preferences.getString(HINT, null);
  }

  @Override public void setHint(@NonNull String hint) {
    preferences.edit().putString(HINT, hint).apply();
  }

  @Override public void clearHint() {
    preferences.edit().remove(HINT).apply();
  }

  @Override public boolean isInfoDialogOnBoard() {
    return preferences.getBoolean(LOCK_DIALOG_ONBOARD, false);
  }

  @Override public long getDefaultIgnoreTime() {
    return Long.parseLong(preferences.getString(ignoreTimeKey, ignoreTimeDefault));
  }

  @Override public long getTimeoutPeriod() {
    return Long.parseLong(preferences.getString(timeoutTimeKey, timeoutTimeDefault));
  }

  @Override public boolean getLockOnPackageChange() {
    return preferences.getBoolean(lockPackageChangeKey, lockPackageChangeDefault);
  }

  @Override public boolean isSystemVisible() {
    return preferences.getBoolean(IS_SYSTEM, false);
  }

  @Override public void setSystemVisible(boolean b) {
    preferences.edit().putBoolean(IS_SYSTEM, b).apply();
  }

  @Nullable @Override public String getMasterPassword() {
    return preferences.getString(MASTER_PASSWORD, null);
  }

  @Override public void setMasterPassword(@NonNull String masterPassword) {
    preferences.edit().putString(MASTER_PASSWORD, masterPassword).apply();
  }

  @Override public void clearMasterPassword() {
    preferences.edit().remove(MASTER_PASSWORD).apply();
  }

  @Override public boolean hasAgreed() {
    return preferences.getBoolean(AGREED, false);
  }

  @Override public void setAgreed() {
    preferences.edit().putBoolean(AGREED, true).apply();
  }

  @Override public boolean isListOnBoard() {
    return preferences.getBoolean(LOCK_LIST_ONBOARD, false);
  }

  @Override public void setListOnBoard() {
    preferences.edit().putBoolean(LOCK_LIST_ONBOARD, true).apply();
  }

  @Override public void setInfoDialogOnBoard() {
    preferences.edit().putBoolean(LOCK_DIALOG_ONBOARD, true).apply();
  }

  @Override public void clearAll() {
    preferences.edit().clear().apply();
  }
}
