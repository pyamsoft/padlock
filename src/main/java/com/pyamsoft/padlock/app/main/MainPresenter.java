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

package com.pyamsoft.padlock.app.main;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.SchedulerPresenter;
import com.pyamsoft.padlock.dagger.main.MainInteractor;
import com.pyamsoft.padlock.model.RxBus;
import com.pyamsoft.padlock.model.event.RefreshEvent;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class MainPresenter extends SchedulerPresenter<MainPresenter.MainView> {

  @NonNull private final MainInteractor interactor;

  @NonNull private Subscription agreeTermsBusSubscription = Subscriptions.empty();
  @NonNull private Subscription refreshBus = Subscriptions.empty();

  @Inject public MainPresenter(@NonNull final MainInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
  }

  @Override public void onResume() {
    super.onResume();
    registerOnAgreeTermsBus();
    registerOnRefreshBus();
  }

  @Override public void onPause() {
    super.onPause();
    unregisterFromAgreeTermsBus();
    unregisterFromRefreshBus();
  }

  private void unregisterFromRefreshBus() {
    if (!refreshBus.isUnsubscribed()) {
      refreshBus.unsubscribe();
    }
  }

  private void registerOnRefreshBus() {
    unregisterFromRefreshBus();
    refreshBus = Bus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(refreshEvent -> {
          getView().forceRefresh();
        }, throwable -> {
          Timber.e(throwable, "RefreshBus onError");
        });
  }

  public final void showTermsDialog() {
    final boolean agreed = interactor.hasAgreed();
    final MainView mainView = getView();
    if (!agreed) {
      mainView.showUsageTermsDialog();
    }
  }

  private void registerOnAgreeTermsBus() {
    unregisterFromAgreeTermsBus();
    agreeTermsBusSubscription = AgreeTermsDialog.Bus.get()
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

  private void unregisterFromAgreeTermsBus() {
    if (!agreeTermsBusSubscription.isUnsubscribed()) {
      agreeTermsBusSubscription.unsubscribe();
    }
  }

  public interface MainView {

    void showUsageTermsDialog();

    void onDidNotAgreeToTerms();

    void forceRefresh();
  }

  public static final class Bus extends RxBus<RefreshEvent> {

    @NonNull private static final Bus instance = new Bus();

    @CheckResult @NonNull public static Bus get() {
      return instance;
    }
  }
}
