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

package com.pyamsoft.padlock.list;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.service.LockServiceStateInteractor;
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockListPresenter extends SchedulerPresenter {

  @NonNull private final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @NonNull private Disposable populateListDisposable = Disposables.empty();
  @NonNull private Disposable systemVisibleDisposable = Disposables.empty();
  @NonNull private Disposable onboardDisposable = Disposables.empty();
  @NonNull private Disposable fabStateDisposable = Disposables.empty();

  @Inject LockListPresenter(@NonNull LockListInteractor lockListInteractor,
      @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override protected void onStop() {
    super.onStop();
    systemVisibleDisposable = DisposableHelper.dispose(systemVisibleDisposable);
    onboardDisposable = DisposableHelper.dispose(onboardDisposable);
    fabStateDisposable = DisposableHelper.dispose(fabStateDisposable);
    populateListDisposable = DisposableHelper.dispose(populateListDisposable);
  }

  public void populateList(@NonNull PopulateListCallback callback, boolean forceRefresh) {
    populateListDisposable = DisposableHelper.dispose(populateListDisposable);
    populateListDisposable = lockListInteractor.populateList(forceRefresh)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .doAfterTerminate(callback::onListPopulated)
        .subscribe(callback::onEntryAddedToList, throwable -> {
          Timber.e(throwable, "populateList onError");
          callback.onListPopulateError();
        });
  }

  public void setFABStateFromPreference(@NonNull FABStateCallback callback) {
    fabStateDisposable = DisposableHelper.dispose(fabStateDisposable);
    fabStateDisposable = stateInteractor.isServiceEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(enabled -> {
          if (enabled) {
            callback.onSetFABStateEnabled();
          } else {
            callback.onSetFABStateDisabled();
          }
        }, throwable -> Timber.e(throwable, "onError"));
  }

  public void setSystemVisible() {
    lockListInteractor.setSystemVisible(true);
  }

  public void setSystemInvisible() {
    lockListInteractor.setSystemVisible(false);
  }

  public void setSystemVisibilityFromPreference(@NonNull SystemVisibilityCallback callback) {
    systemVisibleDisposable = DisposableHelper.dispose(systemVisibleDisposable);
    systemVisibleDisposable = lockListInteractor.isSystemVisible()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(visible -> {
          if (visible) {
            callback.onSetSystemVisible();
          } else {
            callback.onSetSystemInvisible();
          }
        }, throwable -> Timber.e(throwable, "onError"));
  }

  public void showOnBoarding(@NonNull OnboardingCallback callback) {
    onboardDisposable = DisposableHelper.dispose(onboardDisposable);
    onboardDisposable = lockListInteractor.hasShownOnBoarding()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> {
          if (onboard) {
            callback.onOnboardingComplete();
          } else {
            callback.onShowOnboarding();
          }
        }, throwable -> Timber.e(throwable, "onError"));
  }

  interface OnboardingCallback {

    void onShowOnboarding();

    void onOnboardingComplete();
  }

  interface FABStateCallback {

    void onSetFABStateEnabled();

    void onSetFABStateDisabled();
  }

  interface SystemVisibilityCallback {

    void onSetSystemVisible();

    void onSetSystemInvisible();
  }

  interface PopulateListCallback extends LockCommon {

    void onEntryAddedToList(@NonNull AppEntry entry);
  }
}
