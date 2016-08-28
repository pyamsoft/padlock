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

package com.pyamsoft.padlock.app.lock;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.service.PadLockService;
import timber.log.Timber;

public class LockScreenActivity2 extends LockScreenActivity {

  static boolean active = false;

  @CheckResult public static boolean isActive() {
    return active;
  }

  static void setActive(boolean active) {
    Timber.d("Set Active: %s", active);
    LockScreenActivity2.active = active;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Timber.d("LockScreenActivity2 create");
    setActive(true);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    Timber.d("LockScreenActivity2 destroy");
    setActive(false);
  }

  @Override public void onPostUnlock() {
    Timber.d("POST Unlock Finished! 2");
    PadLockService.passLockScreen();
    finish();
  }
}
