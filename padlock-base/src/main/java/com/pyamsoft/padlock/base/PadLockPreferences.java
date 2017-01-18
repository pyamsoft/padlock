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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

public interface PadLockPreferences {

  @CheckResult boolean isIgnoreInKeyguard();

  @CheckResult boolean isInstallListenerEnabled();

  @CheckResult boolean isLockInfoDialogOnBoard();

  void setLockInfoDialogOnBoard();

  @CheckResult long getDefaultIgnoreTime();

  @CheckResult long getTimeoutPeriod();

  @CheckResult boolean getLockOnPackageChange();

  @CheckResult boolean isSystemVisible();

  void setSystemVisible(boolean b);

  @CheckResult String getMasterPassword();

  void setMasterPassword(@NonNull String masterPassword);

  void clearMasterPassword();

  @CheckResult String getHint();

  void setHint(@NonNull String hint);

  void clearHint();

  @CheckResult boolean hasAgreed();

  void setAgreed();

  @CheckResult boolean isOnBoard();

  void setOnBoard();

  void clearAll();
}