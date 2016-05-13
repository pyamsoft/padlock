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
import com.pyamsoft.pydroid.base.ApplicationPreferences;

public class PadLockPreferences extends ApplicationPreferences {

  @NonNull private final String ignoreTimeKey;
  @NonNull private final String ignoreTimeDefault;
  @NonNull private final String timeoutTimeKey;
  @NonNull private final String timeoutTimeDefault;

  @NonNull private static final String IS_SYSTEM = "is_system";
  @NonNull private static final String MASTER_PASSWORD = "master_password";
  @NonNull private static final String AGREED = "agreed";
  @NonNull private static final String ONBOARD = "onboard";

  public PadLockPreferences(final @NonNull Context context) {
    super(context);
    final Context appContext = context.getApplicationContext();
    ignoreTimeKey = appContext.getString(R.string.ignore_time_key);
    ignoreTimeDefault = appContext.getString(R.string.ignore_time_default);
    timeoutTimeKey = appContext.getString(R.string.timeout_time_key);
    timeoutTimeDefault = appContext.getString(R.string.timeout_time_default);
  }

  public final long getDefaultIgnoreTime() {
    return Long.parseLong(get(ignoreTimeKey, ignoreTimeDefault));
  }

  public final long getTimeoutPeriod() {
    return Long.parseLong(get(timeoutTimeKey, timeoutTimeDefault));
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
