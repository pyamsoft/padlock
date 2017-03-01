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

  @NonNull private final PinEntryInteractor interactor;
  @NonNull private Subscription pinEntrySubscription = Subscriptions.empty();
  @NonNull private Subscription pinCheckSubscription = Subscriptions.empty();

  @Inject PinEntryPresenter(@NonNull PinEntryInteractor interactor,
      @Named("obs") Scheduler obsScheduler, @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
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
        .subscribe(event -> {
          boolean creating = (event.type() == PinEntryEvent.Type.TYPE_CREATE);
          if (event.complete()) {
            callback.onSubmitSuccess(creating);
          } else {
            callback.onSubmitFailure(creating);
          }
        }, throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          callback.onSubmitError();
        });
  }

  public void checkMasterPinPresent(@NonNull MasterPinStatusCallback callback) {
    Timber.d("Check if we have a master");
    pinCheckSubscription = SubscriptionHelper.unsubscribe(pinCheckSubscription);
    pinCheckSubscription = interactor.hasMasterPin()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(hasMaster -> {
          if (hasMaster) {
            callback.onMasterPinPresent();
          } else {
            callback.onMasterPinMissing();
          }
        }, throwable -> Timber.e(throwable, "onError checkMasterPinPresent"));
  }

  interface MasterPinStatusCallback {

    void onMasterPinMissing();

    void onMasterPinPresent();
  }

  interface SubmitCallback {

    void onSubmitSuccess(boolean creating);

    void onSubmitFailure(boolean creating);

    void onSubmitError();
  }
}
