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

package com.pyamsoft.padlock.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.Observable;

public abstract class LockHelper {

  @Nullable private static LockHelper INSTANCE;

  public static void set(@NonNull LockHelper interactor) {
    INSTANCE = interactor;
  }

  @CheckResult @NonNull public static LockHelper get() {
    if (INSTANCE == null) {
      throw new IllegalStateException("LockHelper instance is NULL");
    }
    return INSTANCE;
  }

  @NonNull @CheckResult
  public final Observable<Boolean> checkSubmissionAttempt(@NonNull String attempt,
      @NonNull String encodedPin) {
    return encodeSHA256(attempt).map(encodedPin::equals);
  }

  @CheckResult @NonNull public abstract Observable<String> encodeSHA256(@NonNull String attempt);
}
