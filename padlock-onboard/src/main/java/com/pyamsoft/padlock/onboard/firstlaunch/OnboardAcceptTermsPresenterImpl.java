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

package com.pyamsoft.padlock.onboard.firstlaunch;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import timber.log.Timber;

class OnboardAcceptTermsPresenterImpl extends SchedulerPresenter<Presenter.Empty>
    implements OnboardAcceptTermsPresenter {

  @NonNull private final OnboardAcceptTermsInteractor interactor;
  @SuppressWarnings("WeakerAccess") @Nullable Subscription termsSubscription;

  @Inject OnboardAcceptTermsPresenterImpl(@NonNull OnboardAcceptTermsInteractor interactor,
      @NonNull Scheduler observeScheduler, @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(termsSubscription);
  }

  @Override public void acceptUsageTerms(@NonNull UsageTermsCallback callback) {
    SubscriptionHelper.unsubscribe(termsSubscription);
    termsSubscription = interactor.agreeToTerms()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(agreed -> {
              if (agreed) {
                callback.onUsageTermsAccepted();
              }
            }, throwable -> Timber.e(throwable, "onError"),
            () -> SubscriptionHelper.unsubscribe(termsSubscription));
  }
}
