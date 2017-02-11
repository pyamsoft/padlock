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

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.event.PinEntryEvent;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class PinEntryPresenter extends SchedulerPresenter<Presenter.Empty> {

  @SuppressWarnings("WeakerAccess") @NonNull final PinEntryInteractor interactor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription pinEntrySubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription pinCheckSubscription =
      Subscriptions.empty();

  @Inject PinEntryPresenter(@NonNull final PinEntryInteractor interactor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(pinEntrySubscription, pinCheckSubscription);
  }

  public void submit(@NonNull String currentAttempt, @NonNull String reEntryAttempt,
      @NonNull String hint, @NonNull SubmitCallback callback) {
    Timber.d("Attempt PIN submission");
    SubscriptionHelper.unsubscribe(pinEntrySubscription);
    pinEntrySubscription = interactor.getMasterPin()
        .flatMap(masterPin -> {
          if (masterPin == null) {
            return interactor.createPin(currentAttempt, reEntryAttempt, hint);
          } else {
            return interactor.clearPin(masterPin, currentAttempt);
          }
        })
        .filter(pinEntryEvent -> pinEntryEvent != null)
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
        }, () -> SubscriptionHelper.unsubscribe(pinEntrySubscription));
  }

  public void hideUnimportantViews(@NonNull HideViewsCallback callback) {
    Timber.d("Check if we have a master");
    SubscriptionHelper.unsubscribe(pinCheckSubscription);
    pinCheckSubscription = interactor.hasMasterPin()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(hasMaster -> {
          if (hasMaster) {
            callback.hideExtraPinEntryViews();
          } else {
            callback.showExtraPinEntryViews();
          }
        }, throwable -> {
          Timber.e(throwable, "onError hideUnimportantViews");
          // TODO
        }, () -> SubscriptionHelper.unsubscribe(pinCheckSubscription));
  }

  interface HideViewsCallback {

    void showExtraPinEntryViews();

    void hideExtraPinEntryViews();
  }

  interface SubmitCallback extends LockSubmitCallback {

    void handOffPinEvent(@NonNull PinEntryEvent event);
  }
}
