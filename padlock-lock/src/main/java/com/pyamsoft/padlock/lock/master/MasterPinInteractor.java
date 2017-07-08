/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.lock.master;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.base.preference.MasterPinPreferences;
import com.pyamsoft.pydroid.helper.Optional;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton public class MasterPinInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final MasterPinPreferences preferences;

  @Inject MasterPinInteractor(@NonNull MasterPinPreferences preferences) {
    this.preferences = preferences;
  }

  @CheckResult @NonNull public Single<Optional<String>> getMasterPin() {
    return Single.fromCallable(() -> Optional.ofNullable(preferences.getMasterPassword()));
  }

  public void setMasterPin(@Nullable String pin) {
    if (pin == null) {
      preferences.clearMasterPassword();
    } else {
      preferences.setMasterPassword(pin);
    }
  }

  @CheckResult @NonNull public Single<Optional<String>> getHint() {
    return Single.fromCallable(() -> Optional.ofNullable(preferences.getHint()));
  }

  public void setHint(@Nullable String hint) {
    if (hint == null) {
      preferences.clearHint();
    } else {
      preferences.setHint(hint);
    }
  }
}
