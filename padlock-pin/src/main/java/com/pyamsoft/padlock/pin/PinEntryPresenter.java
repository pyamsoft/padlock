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

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.lock.LockSubmitCallback;
import com.pyamsoft.padlock.lock.common.LockTypePresenter;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class PinEntryPresenter extends LockTypePresenter {

  @NonNull private final PinEntryInteractor interactor;
  @NonNull private Subscription pinEntrySubscription = Subscriptions.empty();
  @NonNull private Subscription pinCheckSubscription = Subscriptions.empty();

  @Inject PinEntryPresenter(@NonNull PinEntryInteractor interactor, @NonNull Scheduler obsScheduler,
      @NonNull Scheduler subScheduler) {
    super(interactor, obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    pinCheckSubscription = SubscriptionHelper.unsubscribe(pinCheckSubscription);
    pinEntrySubscription = SubscriptionHelper.unsubscribe(pinEntrySubscription);
  }

  public void submit(@NonNull String currentAttempt, @NonNull String reEntryAttempt,
      @NonNull String hint, @NonNull SubmitCallback callback) {
    Timber.d("Attempt PIN submission");
    pinEntrySubscription = SubscriptionHelper.unsubscribe(pinEntrySubscription);
    pinEntrySubscription = interactor.submitPin(currentAttempt, reEntryAttempt, hint)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(pinEntryEvent -> {
          callback.handOffPinEvent(pinEntryEvent);
          if (pinEntryEvent.complete()) {
            callback.onSubmitSuccess();
          } else {
            callback.onSubmitFailure();
          }
        }, throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          callback.onSubmitError();
        });
  }

  public void hideUnimportantViews(@NonNull HideViewsCallback callback) {
    Timber.d("Check if we have a master");
    pinCheckSubscription = SubscriptionHelper.unsubscribe(pinCheckSubscription);
    pinCheckSubscription = interactor.hasMasterPin()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(hasMaster -> {
          if (hasMaster) {
            callback.hideExtraPinEntryViews();
          } else {
            callback.showExtraPinEntryViews();
          }
        }, throwable -> Timber.e(throwable, "onError hideUnimportantViews"));
  }

  interface HideViewsCallback {

    void showExtraPinEntryViews();

    void hideExtraPinEntryViews();
  }

  interface SubmitCallback extends LockSubmitCallback {

    void handOffPinEvent(@NonNull PinEntryEvent event);
  }
}
