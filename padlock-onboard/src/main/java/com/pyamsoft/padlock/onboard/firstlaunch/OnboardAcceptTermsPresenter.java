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
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class OnboardAcceptTermsPresenter extends SchedulerPresenter {

  @NonNull private final OnboardAcceptTermsInteractor interactor;
  @NonNull private Disposable termsDisposable = Disposables.empty();

  @Inject OnboardAcceptTermsPresenter(@NonNull OnboardAcceptTermsInteractor interactor,
      @Named("obs") Scheduler obsScheduler, @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onStop() {
    super.onStop();
    termsDisposable = DisposableHelper.dispose(termsDisposable);
  }

  /**
   * public
   */
  void acceptUsageTerms(@NonNull UsageTermsCallback callback) {
    termsDisposable = DisposableHelper.dispose(termsDisposable);
    termsDisposable = interactor.agreeToTerms()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(agreed -> {
          if (agreed) {
            callback.onUsageTermsAccepted();
          }
        }, throwable -> Timber.e(throwable, "onError"));
  }

  interface UsageTermsCallback {

    void onUsageTermsAccepted();
  }
}
