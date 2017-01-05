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

package com.pyamsoft.padlock.service;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.pin.MasterPinInteractor;
import javax.inject.Inject;
import rx.Observable;

class LockServiceStateInteractorImpl implements LockServiceStateInteractor {

  @NonNull private final MasterPinInteractor pinInteractor;

  @Inject LockServiceStateInteractorImpl(final @NonNull MasterPinInteractor pinInteractor) {
    this.pinInteractor = pinInteractor;
  }

  @NonNull @CheckResult @Override public Observable<Boolean> isServiceEnabled() {
    return pinInteractor.getMasterPin().map(pin -> pin != null);
  }
}
