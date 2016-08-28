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
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class PinEntryInteractorImpl extends LockInteractorImpl implements PinEntryInteractor {

  @NonNull final MasterPinInteractor masterPinInteractor;

  @Inject PinEntryInteractorImpl(@NonNull final MasterPinInteractor masterPinInteractor) {
    this.masterPinInteractor = masterPinInteractor;
  }

  @Override @CheckResult @NonNull
  public Observable<PinEntryEvent> submitMasterPin(@NonNull String attempt, @NonNull String reentry,
      @NonNull String hint) {
    return masterPinInteractor.getMasterPin().map(masterPin -> {
      if (masterPin == null) {
        return attemptCreatePin(attempt, reentry, hint);
      } else {
        return attemptClearPin(masterPin, attempt);
      }
    });
  }

  @NonNull @Override public Observable<Boolean> hasMasterPin() {
    return masterPinInteractor.getMasterPin().map(s -> s != null);
  }

  @CheckResult @NonNull
  private PinEntryEvent attemptClearPin(@NonNull String masterPin, @NonNull String attempt) {
    final boolean success = checkSubmissionAttempt(attempt, masterPin).toBlocking().first();
    if (success) {
      Timber.d("Clear master pin");
      masterPinInteractor.setMasterPin(null);
      masterPinInteractor.setHint(null);
      return PinEntryEvent.builder().complete(true).type(1).build();
    } else {
      Timber.d("Failed to clear master pin");
      return PinEntryEvent.builder().complete(false).type(1).build();
    }
  }

  @CheckResult @NonNull
  private PinEntryEvent attemptCreatePin(@NonNull String attempt, @NonNull String reentry,
      @NonNull String hint) {
    Timber.d("No existing master pin, attempt to create a new one");
    if (attempt.equals(reentry)) {
      Timber.d("Entry and Re-Entry match, create");
      final String encodedMasterPin = encodeSHA256(attempt).toBlocking().first();
      masterPinInteractor.setMasterPin(encodedMasterPin);

      if (!hint.isEmpty()) {
        Timber.d("User provided hint, save it");
        masterPinInteractor.setHint(hint);
      }

      return PinEntryEvent.builder().complete(true).type(0).build();
    } else {
      Timber.e("Entry and re-entry do not match");
      return PinEntryEvent.builder().complete(false).type(0).build();
    }
  }
}
