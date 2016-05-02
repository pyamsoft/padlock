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
import com.pyamsoft.pydroid.base.PreferenceBase;

public class PadLockPreferences extends PreferenceBase {

  public static final long PERIOD_NONE = 0L;
  public static final long PERIOD_ONE = 1L;
  public static final long PERIOD_FIVE = 5L;
  public static final long PERIOD_TEN = 10L;
  public static final long PERIOD_THIRTY = 30L;
  private static final String TIMEOUT_PERIOD = "timeout_period";
  private static final String IGNORE_TIME = "ignore_time";
  private static final String IS_SYSTEM = "is_system";
  private static final String MASTER_PASSWORD = "master_password";
  private static final String AGREED = "agreed";
  private static final String ONBOARD = "onboard";

  public PadLockPreferences(Context context) {
    super(context);
  }

  public final long getDefaultIgnoreTime() {
    return getLong(PadLockPreferences.IGNORE_TIME, PadLockPreferences.PERIOD_NONE);
  }

  public final void setDefaultIgnoreTime(final long l) {
    putLong(PadLockPreferences.IGNORE_TIME, l);
  }

  public final long getTimeoutPeriod() {
    return getLong(PadLockPreferences.TIMEOUT_PERIOD, PadLockPreferences.PERIOD_FIVE);
  }

  public final void setTimeoutPeriod(long timeoutPeriod) {
    putLong(PadLockPreferences.TIMEOUT_PERIOD, timeoutPeriod);
  }

  public final boolean isSystemVisible() {
    return getBoolean(IS_SYSTEM, false);
  }

  public final void setSystemVisible(final boolean b) {
    putBoolean(IS_SYSTEM, b);
  }

  public final String getMasterPassword() {
    return getString(MASTER_PASSWORD, null);
  }

  public final void setMasterPassword(final String masterPassword) {
    putString(MASTER_PASSWORD, masterPassword);
  }

  public boolean hasAgreed() {
    return getBoolean(AGREED, false);
  }

  public void setAgreed() {
    putBoolean(AGREED, true);
  }

  public boolean isOnBoard() {
    return getBoolean(ONBOARD, false);
  }

  public void setOnBoard() {
    putBoolean(ONBOARD, true);
  }
}
