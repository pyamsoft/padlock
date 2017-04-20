/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.list;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockInfoPresenter extends SchedulerPresenter {

  @NonNull private final LockInfoInteractor lockInfoInteractor;

  @Inject LockInfoPresenter(final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("obs") Scheduler obsScheduler,
      final @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.lockInfoInteractor = lockInfoInteractor;
  }

  /**
   * public
   */
  void populateList(@NonNull String packageName, boolean forceRefresh,
      @NonNull PopulateListCallback callback) {
    disposeOnStop(lockInfoInteractor.populateList(packageName, forceRefresh)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .doAfterTerminate(callback::onListPopulated)
        .doOnSubscribe(disposable -> callback.onListPopulateBegin())
        .subscribe(callback::onEntryAddedToList, throwable -> {
          Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
          callback.onListPopulateError();
        }));
  }

  /**
   * public
   */
  void showOnBoarding(@NonNull OnBoardingCallback callback) {
    disposeOnStop(lockInfoInteractor.hasShownOnBoarding()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> {
          if (onboard) {
            callback.onOnboardingComplete();
          } else {
            callback.onShowOnboarding();
          }
        }, throwable -> Timber.e(throwable, "onError")));
  }

  interface OnBoardingCallback {

    void onShowOnboarding();

    void onOnboardingComplete();
  }

  interface PopulateListCallback extends LockCommon {

    void onEntryAddedToList(@NonNull ActivityEntry entry);
  }
}
