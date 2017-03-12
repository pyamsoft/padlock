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
import com.pyamsoft.padlock.lock.master.MasterPinInteractor;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton public class LockServiceStateInteractor {

  @NonNull private final MasterPinInteractor pinInteractor;

  @Inject LockServiceStateInteractor(@NonNull MasterPinInteractor pinInteractor) {
    this.pinInteractor = pinInteractor;
  }

  @NonNull @CheckResult public Observable<Boolean> isServiceEnabled() {
    return pinInteractor.getMasterPin().isEmpty().map(aBoolean -> !aBoolean).toObservable();
  }
}
