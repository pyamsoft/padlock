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

final class PadLockPreferencesImpl extends ApplicationPreferences implements PadLockPreferences {

  @NonNull private static final String IS_SYSTEM = "is_system";
  @NonNull private static final String MASTER_PASSWORD = "master_password";
  @NonNull private static final String AGREED = "agreed";
  @NonNull private static final String ONBOARD = "onboard";
  @NonNull private final String ignoreTimeKey;
  @NonNull private final String ignoreTimeDefault;
  @NonNull private final String timeoutTimeKey;
  @NonNull private final String timeoutTimeDefault;
  @NonNull private final String lockPackageChangeKey;
  @NonNull private final String lockDeviceLockedKey;
  private final boolean lockPackageChangeDefault;
  private final boolean lockDeviceLockedDefault;

  @Inject public PadLockPreferencesImpl(final @NonNull Context context) {
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
