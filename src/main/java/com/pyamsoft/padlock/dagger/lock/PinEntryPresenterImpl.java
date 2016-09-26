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

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.lock.PinEntryPresenter;
import com.pyamsoft.padlock.app.lock.PinScreen;
import com.pyamsoft.padlock.bus.PinEntryBus;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class PinEntryPresenterImpl extends LockPresenterImpl<PinScreen> implements PinEntryPresenter {

  @NonNull final AppIconLoaderPresenter<PinScreen> iconLoader;
  @NonNull private final PinEntryInteractor interactor;
  @NonNull private Subscription pinEntrySubscription = Subscriptions.empty();
  @NonNull private Subscription pinCheckSubscription = Subscriptions.empty();

  @Inject PinEntryPresenterImpl(@NonNull AppIconLoaderPresenter<PinScreen> iconLoader,
      @NonNull final PinEntryInteractor interactor, @NonNull @Named("obs") Scheduler obsScheduler,
      @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.iconLoader = iconLoader;
    this.interactor = interactor;
  }

  @SuppressWarnings("WeakerAccess") void unsubPinEntry() {
    if (!pinEntrySubscription.isUnsubscribed()) {
      pinEntrySubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubPinCheck() {
    if (!pinCheckSubscription.isUnsubscribed()) {
      pinCheckSubscription.unsubscribe();
    }
  }

  @Override protected void onBind() {
    super.onBind();
    getView(iconLoader::bindView);
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    iconLoader.unbindView();
    unsubPinEntry();
    unsubPinCheck();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    iconLoader.destroy();
  }

  @Override public void submit(@NonNull String currentAttempt, @NonNull String reEntryAttempt,
      @NonNull String hint) {
    Timber.d("Attempt PIN submission");
    unsubPinEntry();
    pinEntrySubscription = interactor.submitMasterPin(currentAttempt, reEntryAttempt, hint)
        .filter(pinEntryEvent -> pinEntryEvent != null)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(pinEntryEvent -> {
          PinEntryBus.get().post(pinEntryEvent);
          getView(pinScreen -> {
            if (pinEntryEvent.complete()) {
              pinScreen.onSubmitSuccess();
            } else {
              pinScreen.onSubmitFailure();
            }
          });
        }, throwable -> {
          Timber.e(throwable, "attemptPinSubmission onError");
          getView(PinScreen::onSubmitError);
        }, this::unsubPinEntry);
  }

  @Override public void hideUnimportantViews() {
    Timber.d("Check if we have a master");
    unsubPinCheck();
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
        }, this::unsubPinCheck);
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    iconLoader.loadApplicationIcon(packageName);
  }
}
