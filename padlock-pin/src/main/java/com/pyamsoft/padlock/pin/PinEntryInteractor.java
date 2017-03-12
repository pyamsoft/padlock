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

package com.pyamsoft.padlock.pin;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.lock.LockHelper;
import com.pyamsoft.padlock.lock.common.LockTypeInteractor;
import com.pyamsoft.padlock.lock.master.MasterPinInteractor;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.pydroid.function.OptionalWrapper;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton class PinEntryInteractor extends LockTypeInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final MasterPinInteractor masterPinInteractor;

  @Inject PinEntryInteractor(@NonNull MasterPinInteractor masterPinInteractor,
      @NonNull PadLockPreferences preferences) {
    super(preferences);
    this.masterPinInteractor = masterPinInteractor;
  }

  @NonNull @CheckResult public Observable<Boolean> hasMasterPin() {
    return getMasterPin().map(OptionalWrapper::isPresent);
  }

  @NonNull @CheckResult public Observable<PinEntryEvent> submitPin(@NonNull String currentAttempt,
      @NonNull String reEntryAttempt, @NonNull String hint) {
    return getMasterPin().flatMap(masterPin -> {
      String pin = masterPin.item();
      if (pin == null) {
        return createPin(currentAttempt, reEntryAttempt, hint);
      } else {
        return clearPin(pin, currentAttempt);
      }
    });
  }

  @CheckResult @NonNull private Observable<OptionalWrapper<String>> getMasterPin() {
    return masterPinInteractor.getMasterPin();
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<PinEntryEvent> clearPin(
      @NonNull String masterPin, @NonNull String attempt) {
    return LockHelper.get().checkSubmissionAttempt(attempt, masterPin).map(success -> {
      if (success) {
        Timber.d("Clear master item");
        masterPinInteractor.setMasterPin(null);
        masterPinInteractor.setHint(null);
      } else {
        Timber.d("Failed to clear master item");
      }

      return PinEntryEvent.create(PinEntryEvent.Type.TYPE_CLEAR, success);
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<PinEntryEvent> createPin(
      @NonNull String attempt, @NonNull String reentry, @NonNull String hint) {
    return Observable.fromCallable(() -> {
      Timber.d("No existing master item, attempt to create a new one");

      final boolean success = attempt.equals(reentry);
      if (success) {
        Timber.d("Entry and Re-Entry match, create");
        final String encodedMasterPin = LockHelper.get().encodeSHA256(attempt).blockingFirst();
        masterPinInteractor.setMasterPin(encodedMasterPin);

        if (!hint.isEmpty()) {
          Timber.d("User provided hint, save it");
          masterPinInteractor.setHint(hint);
        }
      } else {
        Timber.e("Entry and re-entry do not match");
      }

      return PinEntryEvent.create(PinEntryEvent.Type.TYPE_CREATE, success);
    });
  }
}
