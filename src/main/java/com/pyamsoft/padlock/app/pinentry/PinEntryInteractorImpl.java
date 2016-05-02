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

package com.pyamsoft.padlock.app.pinentry;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.lock.LockInteractor;
import com.pyamsoft.padlock.app.pin.MasterPinInteractor;
import com.pyamsoft.padlock.app.pin.PinUtils;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

public class PinEntryInteractorImpl implements PinEntryInteractor {

  @NonNull private final LockInteractor lockInteractor;
  @NonNull private final MasterPinInteractor masterPinInteractor;

  @Inject public PinEntryInteractorImpl(@NonNull final MasterPinInteractor masterPinInteractor,
      final @NonNull LockInteractor lockInteractor) {
    this.masterPinInteractor = masterPinInteractor;
    this.lockInteractor = lockInteractor;
  }

  @Override public boolean isSubmittable(String attempt) {
    return lockInteractor.isSubmittable(attempt);
  }

  @NonNull @Override public Observable<PinEntryEvent> submitMasterPin(String attempt) {
    return Observable.defer(() -> Observable.just(masterPinInteractor.getMasterPin()))
        .map(masterPin -> {
          final String encodedMasterPin = PinUtils.hash256(attempt);
          if (masterPin == null) {
            Timber.d("No existing master pin, create a new one");
            masterPinInteractor.setMasterPin(encodedMasterPin);
            return PinEntryEvent.builder().complete(true).type(0).build();
          } else {
            final boolean success = lockInteractor.compareAttemptToPIN(encodedMasterPin,
                masterPinInteractor.getMasterPin());
            if (success) {
              Timber.d("Clear master pin");
              masterPinInteractor.setMasterPin(null);
              return PinEntryEvent.builder().complete(true).type(1).build();
            } else {
              Timber.d("Failed to clear master pin");
              return PinEntryEvent.builder().complete(false).type(1).build();
            }
          }
        });
  }
}
