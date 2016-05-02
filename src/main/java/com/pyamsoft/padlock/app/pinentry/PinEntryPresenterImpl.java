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

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.lock.LockInteractor;
import com.pyamsoft.padlock.app.lock.LockPresenterImpl;
import com.pyamsoft.padlock.model.event.LockButtonClickEvent;
import javax.inject.Inject;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class PinEntryPresenterImpl extends LockPresenterImpl<PinScreen>
    implements PinEntryPresenter {

  @NonNull private final PinEntryInteractor interactor;

  @NonNull private Subscription pinEntrySubscription = Subscriptions.empty();

  @Inject
  public PinEntryPresenterImpl(final Context context, @NonNull final LockInteractor lockInteractor,
      @NonNull final PinEntryInteractor interactor) {
    super(context.getApplicationContext(), lockInteractor);
    this.interactor = interactor;
  }

  private void unsubPinEntry() {
    if (!pinEntrySubscription.isUnsubscribed()) {
      pinEntrySubscription.unsubscribe();
    }
  }

  @Override public void unbind() {
    super.unbind();
    unsubPinEntry();
  }

  @Override protected LockButtonClickEvent takeCommand() throws NullPointerException {
    final PinScreen pinScreen = get();
    final String attempt = pinScreen.getCurrentAttempt();
    final boolean isSubmittable = interactor.isSubmittable(attempt);
    return LockButtonClickEvent.builder()
        .status(isSubmittable ? LockButtonClickEvent.STATUS_SUBMITTABLE
            : LockButtonClickEvent.STATUS_ERROR)
        .code(attempt)
        .build();
  }

  @Override protected LockButtonClickEvent clickButton(char button) throws NullPointerException {
    final PinScreen pinScreen = get();
    final String attempt = pinScreen.getCurrentAttempt();
    final String code = buttonClicked(button, attempt);
    return LockButtonClickEvent.builder()
        .status(LockButtonClickEvent.STATUS_NONE)
        .code(code)
        .build();
  }

  @Override public void attemptPinSubmission() {
    Timber.d("Attempt PIN submission");
    unsubPinEntry();
    pinEntrySubscription = interactor.submitMasterPin(get().getCurrentAttempt())
        .filter(pinEntryEvent -> pinEntryEvent != null)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pinEntryEvent -> {
          get().onSubmissionComplete();
          PinEntryDialog.PinEntryBus.get().post(pinEntryEvent);
        }, throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          get().onErrorButtonEvent();
        });
  }
}
