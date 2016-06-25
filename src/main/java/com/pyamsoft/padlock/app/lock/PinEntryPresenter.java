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

package com.pyamsoft.padlock.app.lock;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.dagger.lock.PinEntryInteractor;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class PinEntryPresenter extends LockPresenter<PinScreen> {

  @NonNull private final PinEntryInteractor interactor;
  @NonNull private Subscription pinEntrySubscription = Subscriptions.empty();

  @Inject public PinEntryPresenter(@NonNull final PinEntryInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
  }

  private void unsubPinEntry() {
    if (!pinEntrySubscription.isUnsubscribed()) {
      pinEntrySubscription.unsubscribe();
    }
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubPinEntry();
  }

  public final void submit(@NonNull String currentAttempt) {
    Timber.d("Attempt PIN submission");
    unsubPinEntry();
    final PinScreen pinScreen = getView();
    pinEntrySubscription = interactor.submitMasterPin(currentAttempt)
        .filter(pinEntryEvent -> pinEntryEvent != null)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(pinEntryEvent -> {
          PinEntryDialog.PinEntryBus.get().post(pinEntryEvent);
          if (pinEntryEvent.complete()) {
            pinScreen.onSubmitSuccess();
          } else {
            pinScreen.onSubmitFailure();
          }
        }, throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          pinScreen.onSubmitError();
        });
  }
}
