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
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.padlock.pin.MasterPinInteractor;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class PinEntryInteractor extends LockInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final MasterPinInteractor masterPinInteractor;

  @Inject PinEntryInteractor(@NonNull final MasterPinInteractor masterPinInteractor) {
    this.masterPinInteractor = masterPinInteractor;
  }

  @NonNull @CheckResult public Observable<Boolean> hasMasterPin() {
    return getMasterPin().map(s -> s != null);
  }

  @NonNull @CheckResult public Observable<String> getMasterPin() {
    return masterPinInteractor.getMasterPin();
  }

  @CheckResult @NonNull
  public Observable<PinEntryEvent> clearPin(@NonNull String masterPin, @NonNull String attempt) {
    return checkSubmissionAttempt(attempt, masterPin).map(success -> {

      if (success) {
        Timber.d("Clear master pin");
        masterPinInteractor.setMasterPin(null);
        masterPinInteractor.setHint(null);
      } else {
        Timber.d("Failed to clear master pin");
      }

      return PinEntryEvent.builder().complete(success).type(1).build();
    });
  }

  @CheckResult @NonNull
  public Observable<PinEntryEvent> createPin(@NonNull String attempt, @NonNull String reentry,
      @NonNull String hint) {
    return Observable.fromCallable(() -> {
      Timber.d("No existing master pin, attempt to create a new one");

      final boolean success = attempt.equals(reentry);
      if (success) {
        Timber.d("Entry and Re-Entry match, create");
        final String encodedMasterPin = encodeSHA256(attempt).toBlocking().first();
        masterPinInteractor.setMasterPin(encodedMasterPin);

        if (!hint.isEmpty()) {
          Timber.d("User provided hint, save it");
          masterPinInteractor.setHint(hint);
        }
      } else {
        Timber.e("Entry and re-entry do not match");
      }

      return PinEntryEvent.builder().complete(success).type(0).build();
    });
  }
}
