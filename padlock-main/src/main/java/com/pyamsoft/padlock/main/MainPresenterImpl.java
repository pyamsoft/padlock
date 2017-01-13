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

package com.pyamsoft.padlock.main;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import timber.log.Timber;

class MainPresenterImpl extends SchedulerPresenter<MainPresenter.MainView>
    implements MainPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final MainInteractor interactor;
  @SuppressWarnings("WeakerAccess") @Nullable Subscription onboardingSubscription;

  @Inject MainPresenterImpl(@NonNull final MainInteractor interactor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onBind() {
    super.onBind();
    showOnboardingOrDefault();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(onboardingSubscription);
  }

  @Override public void showOnboardingOrDefault() {
    SubscriptionHelper.unsubscribe(onboardingSubscription);
    onboardingSubscription = interactor.isOnboardingComplete()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboardingComplete -> getView(mainView -> {
              if (onboardingComplete) {
                mainView.onShowDefaultPage();
              } else {
                mainView.onShowOnboarding();
              }
            }), throwable -> Timber.e(throwable, "onError"),
            () -> SubscriptionHelper.unsubscribe(onboardingSubscription));
  }

  //@Override public void showTermsDialog() {
  //  SubscriptionHelper.unsubscribe(onboardingSubscription);
  //  onboardingSubscription = interactor.hasAgreed()
  //      .subscribeOn(getSubscribeScheduler())
  //      .observeOn(getObserveScheduler())
  //      .subscribe(agreed -> {
  //        if (!agreed) {
  //          getView(MainView::showUsageTermsDialog);
  //        }
  //      }, throwable -> {
  //        Timber.e(throwable, "onError");
  //        // TODO error
  //      }, () -> SubscriptionHelper.unsubscribe(onboardingSubscription));
  //}

  //@Override public void agreeToTerms(boolean agreed) {
  //  if (agreed) {
  //    interactor.setAgreed();
  //  } else {
  //    getView(MainView::onDidNotAgreeToTerms);
  //  }
  //}
}
