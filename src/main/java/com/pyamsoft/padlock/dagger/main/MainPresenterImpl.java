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
import com.pyamsoft.padlock.app.main.MainPresenter;
import com.pyamsoft.padlock.bus.AgreeTermsBus;
import com.pyamsoft.padlock.bus.MainBus;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
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
      @NonNull @Named("obs") Scheduler obsScheduler,
      @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onBind() {
    super.onBind();
    registerOnAgreeTermsBus();
    registerOnRefreshBus();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unregisterFromAgreeTermsBus();
    unregisterFromRefreshBus();
    unsubscribeAgreeTerms();
  }

  void unregisterFromRefreshBus() {
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
        .subscribe(refreshEvent -> getView(MainView::forceRefresh),
            throwable -> Timber.e(throwable, "RefreshBus onError"), this::unregisterFromRefreshBus);
  }

  @Override public void showTermsDialog() {
    unsubscribeAgreeTerms();
    agreeTermsSubscription = interactor.hasAgreed()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(agreed -> {
          if (!agreed) {
            getView(MainView::showUsageTermsDialog);
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
            getView(MainView::onDidNotAgreeToTerms);
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
