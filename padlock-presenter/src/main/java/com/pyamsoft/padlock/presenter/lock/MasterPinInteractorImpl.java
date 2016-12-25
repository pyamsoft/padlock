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

package com.pyamsoft.padlock.presenter.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.presenter.PadLockPreferences;
import javax.inject.Inject;
import rx.Observable;

class MasterPinInteractorImpl implements MasterPinInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;

  @Inject MasterPinInteractorImpl(@NonNull final PadLockPreferences preferences) {
    this.preferences = preferences;
  }

  @CheckResult @NonNull @Override public Observable<String> getMasterPin() {
    return Observable.defer(() -> Observable.just(preferences.getMasterPassword()));
  }

  @Override public void setMasterPin(@Nullable String pin) {
    preferences.setMasterPassword(pin);
  }

  @CheckResult @NonNull @Override public Observable<String> getHint() {
    return Observable.defer(() -> Observable.just(preferences.getHint()));
  }

  @Override public void setHint(@Nullable String hint) {
    preferences.setHint(hint);
  }
}
