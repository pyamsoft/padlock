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

package com.pyamsoft.padlock.dagger.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import rx.Observable;

public interface PinEntryInteractor extends LockInteractor {

  @CheckResult @NonNull Observable<Boolean> hasMasterPin();

  @CheckResult @NonNull Observable<String> getMasterPin();

  @CheckResult @NonNull Observable<PinEntryEvent> clearPin(@NonNull String masterPin,
      @NonNull String attempt);

  @CheckResult @NonNull Observable<PinEntryEvent> createPin(@NonNull String attempt,
      @NonNull String reentry, @NonNull String hint);
}
