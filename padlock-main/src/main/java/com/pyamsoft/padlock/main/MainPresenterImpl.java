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

  @Override public void showOnboardingOrDefault(@NonNull OnboardingCallback callback) {
    SubscriptionHelper.unsubscribe(onboardingSubscription);
    onboardingSubscription = interactor.isOnboardingComplete()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboardingComplete -> {
              if (onboardingComplete) {
                callback.onShowDefaultPage();
              } else {
                callback.onShowOnboarding();
              }
            }, throwable -> Timber.e(throwable, "onError"),
            () -> SubscriptionHelper.unsubscribe(onboardingSubscription));
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(onboardingSubscription);
  }
}
