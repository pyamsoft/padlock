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

package com.pyamsoft.padlock.lock.common;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.model.LockScreenType;
import javax.inject.Inject;
import rx.Observable;

public class LockTypeInteractor {

  @NonNull private final PadLockPreferences preferences;

  @Inject protected LockTypeInteractor(@NonNull PadLockPreferences preferences) {
    this.preferences = preferences;
  }

  @NonNull @CheckResult protected PadLockPreferences getPreferences() {
    return preferences;
  }

  @CheckResult @NonNull public Observable<LockScreenType> getLockScreenType() {
    return Observable.fromCallable(() -> LockScreenType.TYPE_PATTERN);
  }
}
