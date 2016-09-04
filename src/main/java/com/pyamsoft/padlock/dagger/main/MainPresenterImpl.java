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

package com.pyamsoft.padlock.dagger.main;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.pyamsoft.padlock.app.bus.AgreeTermsBus;
import com.pyamsoft.padlock.app.bus.MainBus;
import com.pyamsoft.padlock.app.main.MainPresenter;
import com.pyamsoft.pydroid.app.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class MainPresenterImpl extends SchedulerPresenter<MainPresenter.MainView>
    implements MainPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final MainInteractor interactor;

  @NonNull private Subscription agreeTermsBusSubscription = Subscriptions.empty();
  @NonNull private Subscription refreshBus = Subscriptions.empty();
  @NonNull private Subscription agreeTermsSubscription = Subscriptions.empty();

  @Inject MainPresenterImpl(@NonNull final MainInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
  }

  @Override protected void onBind(@NonNull MainView view) {
    super.onBind(view);
    registerOnAgreeTermsBus();
    registerOnRefreshBus();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unregisterFromAgreeTermsBus();
    unregisterFromRefreshBus();
    unsubscribeAgreeTerms();
  }

  private void unregisterFromRefreshBus() {
    if (!refreshBus.isUnsubscribed()) {
      refreshBus.unsubscribe();
    }
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess") void registerOnRefreshBus() {
    unregisterFromRefreshBus();
    refreshBus = MainBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(refreshEvent -> {
          getView().forceRefresh();
        }, throwable -> {
          Timber.e(throwable, "RefreshBus onError");
        });
  }

  @Override public void showTermsDialog() {
    unsubscribeAgreeTerms();
    agreeTermsSubscription = interactor.hasAgreed()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(agreed -> {
          final MainView mainView = getView();
          if (!agreed) {
            mainView.showUsageTermsDialog();
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          // TODO error
        }, this::unsubscribeAgreeTerms);
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess") void registerOnAgreeTermsBus() {
    unregisterFromAgreeTermsBus();
    agreeTermsBusSubscription = AgreeTermsBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(agreeTermsEvent -> {
          if (agreeTermsEvent.agreed()) {
            interactor.setAgreed();
          } else {
            getView().onDidNotAgreeToTerms();
          }
        }, throwable -> {
          Timber.e(throwable, "AgreeTermsBus onError");
        });
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeAgreeTerms() {
    if (!agreeTermsSubscription.isUnsubscribed()) {
      agreeTermsSubscription.unsubscribe();
    }
  }

  private void unregisterFromAgreeTermsBus() {
    if (!agreeTermsBusSubscription.isUnsubscribed()) {
      agreeTermsBusSubscription.unsubscribe();
    }
  }
}
