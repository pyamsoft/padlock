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
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class MainPresenter extends SchedulerPresenter<MainPresenter.MainView> {

  @NonNull private final MainInteractor interactor;
  @NonNull private Disposable onboardingDisposable = Disposables.empty();

  @Inject MainPresenter(@NonNull MainInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  public void showOnboardingOrDefault(@NonNull OnboardingCallback callback) {
    onboardingDisposable = DisposableHelper.dispose(onboardingDisposable);
    onboardingDisposable = interactor.isOnboardingComplete()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboardingComplete -> {
          if (onboardingComplete) {
            callback.onShowDefaultPage();
          } else {
            callback.onShowOnboarding();
          }
        }, throwable -> Timber.e(throwable, "onError"));
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    onboardingDisposable = DisposableHelper.dispose(onboardingDisposable);
  }

  interface OnboardingCallback {

    void onShowOnboarding();

    void onShowDefaultPage();
  }

  interface MainView {

    void onForceRefresh();
  }
}
