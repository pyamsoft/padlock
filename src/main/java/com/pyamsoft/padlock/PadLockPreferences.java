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

package com.pyamsoft.padlock;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.pydroid.base.PreferenceBase;

public class PadLockPreferences extends PreferenceBase {

  @NonNull private static final String TIMEOUT_PERIOD = "timeout_period";
  @NonNull private static final String IGNORE_TIME = "ignore_time";
  @NonNull private static final String IS_SYSTEM = "is_system";
  @NonNull private static final String MASTER_PASSWORD = "master_password";
  @NonNull private static final String AGREED = "agreed";
  @NonNull private static final String ONBOARD = "onboard";

  public static final long PERIOD_NONE = 0L;
  public static final long PERIOD_ONE = 1L;
  public static final long PERIOD_FIVE = 5L;
  public static final long PERIOD_TEN = 10L;
  public static final long PERIOD_THIRTY = 30L;

  public PadLockPreferences(final @NonNull Context context) {
    super(context);
  }

  public final long getDefaultIgnoreTime() {
    return get(PadLockPreferences.IGNORE_TIME, PadLockPreferences.PERIOD_NONE);
  }

  public final void setDefaultIgnoreTime(final long l) {
    put(PadLockPreferences.IGNORE_TIME, l);
  }

  public final long getTimeoutPeriod() {
    return get(PadLockPreferences.TIMEOUT_PERIOD, PadLockPreferences.PERIOD_FIVE);
  }

  public final void setTimeoutPeriod(long timeoutPeriod) {
    put(PadLockPreferences.TIMEOUT_PERIOD, timeoutPeriod);
  }

  public final boolean isSystemVisible() {
    return get(IS_SYSTEM, false);
  }

  public final void setSystemVisible(final boolean b) {
    put(IS_SYSTEM, b);
  }

  public final String getMasterPassword() {
    return get(MASTER_PASSWORD, null);
  }

  public final void setMasterPassword(final @Nullable String masterPassword) {
    put(MASTER_PASSWORD, masterPassword);
  }

  public boolean hasAgreed() {
    return get(AGREED, false);
  }

  public void setAgreed() {
    put(AGREED, true);
  }

  public boolean isOnBoard() {
    return get(ONBOARD, false);
  }

  public void setOnBoard() {
    put(ONBOARD, true);
  }
}
