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

package com.pyamsoft.presenter.lock;

import android.support.annotation.NonNull;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import com.pyamsoft.pydroidrx.SubscriptionHelper;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class PinEntryPresenterImpl extends SchedulerPresenter<PinScreen>
    implements PinEntryPresenter, LockPresenter<PinScreen> {

  @SuppressWarnings("WeakerAccess") @NonNull final PinEntryInteractor interactor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription pinEntrySubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription pinCheckSubscription =
      Subscriptions.empty();

  @Inject PinEntryPresenterImpl(@NonNull final PinEntryInteractor interactor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(pinEntrySubscription, pinCheckSubscription);
  }

  @Override public void submit(@NonNull String currentAttempt, @NonNull String reEntryAttempt,
      @NonNull String hint) {
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
        .subscribe(pinEntryEvent -> getView(pinScreen -> {
          pinScreen.handOffPinEvent(pinEntryEvent);
          if (pinEntryEvent.complete()) {
            pinScreen.onSubmitSuccess();
          } else {
            pinScreen.onSubmitFailure();
          }
        }), throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          getView(PinScreen::onSubmitError);
        }, () -> SubscriptionHelper.unsubscribe(pinEntrySubscription));
  }

  @Override public void hideUnimportantViews() {
    Timber.d("Check if we have a master");
    SubscriptionHelper.unsubscribe(pinCheckSubscription);
    pinCheckSubscription = interactor.hasMasterPin()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(hasMaster -> getView(pinScreen -> {
          if (hasMaster) {
            pinScreen.hideExtraPinEntryViews();
          } else {
            pinScreen.showExtraPinEntryViews();
          }
        }), throwable -> {
          Timber.e(throwable, "onError hideUnimportantViews");
          // TODO
        }, () -> SubscriptionHelper.unsubscribe(pinCheckSubscription));
  }
}
