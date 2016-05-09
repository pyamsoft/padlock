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

package com.pyamsoft.padlock.dagger.pinentry;

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.pinentry.PinEntryDialog;
import com.pyamsoft.padlock.app.pinentry.PinEntryInteractor;
import com.pyamsoft.padlock.app.pinentry.PinEntryPresenter;
import com.pyamsoft.padlock.app.pinentry.PinScreen;
import com.pyamsoft.padlock.dagger.lock.LockPresenterImpl;
import javax.inject.Inject;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class PinEntryPresenterImpl extends LockPresenterImpl<PinScreen>
    implements PinEntryPresenter {

  @NonNull private final PinEntryInteractor interactor;

  @NonNull private Subscription pinEntrySubscription = Subscriptions.empty();

  @Inject public PinEntryPresenterImpl(final Context context,
      @NonNull final PinEntryInteractor interactor) {
    super(context.getApplicationContext(), interactor);
    this.interactor = interactor;
  }

  private void unsubPinEntry() {
    if (!pinEntrySubscription.isUnsubscribed()) {
      pinEntrySubscription.unsubscribe();
    }
  }

  @Override public void stop() {
    super.stop();
    unsubPinEntry();
  }

  @Override public void submit() {
    Timber.d("Attempt PIN submission");
    unsubPinEntry();
    pinEntrySubscription = interactor.submitMasterPin(get().getCurrentAttempt())
        .filter(pinEntryEvent -> pinEntryEvent != null)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pinEntryEvent -> {
          if (pinEntryEvent.complete()) {
            get().onSubmitSuccess();
            PinEntryDialog.PinEntryBus.get().post(pinEntryEvent);
          } else {
            get().onSubmitFailure();
          }
        }, throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          get().onSubmitError();
        });
  }
}
