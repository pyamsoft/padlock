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

package com.pyamsoft.padlock.dagger;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.R;
import com.pyamsoft.pydroid.base.app.ApplicationPreferences;
import javax.inject.Inject;

class PadLockPreferencesImpl extends ApplicationPreferences implements PadLockPreferences {

  @NonNull static final String IS_SYSTEM = "is_system";
  @NonNull static final String MASTER_PASSWORD = "master_password";
  @NonNull static final String HINT = "hint";
  @NonNull static final String AGREED = "agreed";
  @NonNull static final String ONBOARD = "onboard";
  @NonNull final String ignoreTimeKey;
  @NonNull final String ignoreTimeDefault;
  @NonNull final String timeoutTimeKey;
  @NonNull final String timeoutTimeDefault;
  @NonNull final String lockPackageChangeKey;
  @NonNull final String lockDeviceLockedKey;
  @NonNull final String nSupportKey;
  @NonNull final String recheckKey;
  final boolean lockPackageChangeDefault;
  final boolean lockDeviceLockedDefault;
  final boolean recheckDefault;
  final boolean nSupportDefault;

  @Inject PadLockPreferencesImpl(final @NonNull Context context) {
    super(context);
    final Context appContext = context.getApplicationContext();
    final Resources res = appContext.getResources();
    ignoreTimeKey = appContext.getString(R.string.ignore_time_key);
    ignoreTimeDefault = appContext.getString(R.string.ignore_time_default);
    timeoutTimeKey = appContext.getString(R.string.timeout_time_key);
    timeoutTimeDefault = appContext.getString(R.string.timeout_time_default);
    lockPackageChangeKey = appContext.getString(R.string.lock_package_change_key);
    lockPackageChangeDefault = res.getBoolean(R.bool.lock_package_change_default);
    lockDeviceLockedKey = appContext.getString(R.string.lock_device_locked_key);
    lockDeviceLockedDefault = res.getBoolean(R.bool.lock_device_locked_default);
    recheckKey = appContext.getString(R.string.recheck_key);
    recheckDefault = res.getBoolean(R.bool.recheck_default);
    nSupportKey = appContext.getString(R.string.n_support_key);
    nSupportDefault = res.getBoolean(R.bool.n_support_default);
  }

  @Override public String getHint() {
    return get(HINT, null);
  }

  @Override public void setHint(@Nullable String hint) {
    put(HINT, hint);
  }

  @Override public boolean isRecheckEnabled() {
    return get(recheckKey, recheckDefault);
  }

  @Override public boolean isExperimentalNSupported() {
    return get(nSupportKey, nSupportDefault);
  }

  @Override @CheckResult public final boolean getLockOnDeviceLocked() {
    return get(lockDeviceLockedKey, lockDeviceLockedDefault);
  }

  @Override @CheckResult public final long getDefaultIgnoreTime() {
    return Long.parseLong(get(ignoreTimeKey, ignoreTimeDefault));
  }

  @Override @CheckResult public final long getTimeoutPeriod() {
    return Long.parseLong(get(timeoutTimeKey, timeoutTimeDefault));
  }

  @Override @CheckResult public final boolean getLockOnPackageChange() {
    return get(lockPackageChangeKey, lockPackageChangeDefault);
  }

  @Override @CheckResult public final boolean isSystemVisible() {
    return get(IS_SYSTEM, false);
  }

  @Override public final void setSystemVisible(final boolean b) {
    put(IS_SYSTEM, b);
  }

  @Override @CheckResult public final String getMasterPassword() {
    return get(MASTER_PASSWORD, null);
  }

  @Override public final void setMasterPassword(final @Nullable String masterPassword) {
    put(MASTER_PASSWORD, masterPassword);
  }

  @Override @CheckResult public boolean hasAgreed() {
    return get(AGREED, false);
  }

  @Override public void setAgreed() {
    put(AGREED, true);
  }

  @Override @CheckResult public boolean isOnBoard() {
    return get(ONBOARD, false);
  }

  @Override public void setOnBoard() {
    put(ONBOARD, true);
  }

  @Override public void clearAll() {
    clear(true);
  }
}
