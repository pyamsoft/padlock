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
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.pin.ClearPinEvent;
import com.pyamsoft.padlock.pin.CreatePinEvent;
import com.pyamsoft.padlock.service.LockServiceStateInteractor;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockListPresenter extends SchedulerPresenter {

  @NonNull private final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;

  @Inject LockListPresenter(@NonNull LockListInteractor lockListInteractor,
      @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  /**
   * public
   */
  void registerOnBus(@NonNull BusCallback callback) {
    //disposeOnStop(EventBus.get()
    //    .listen(CreatePinEvent.class)
    //    .subscribeOn(getBackgroundScheduler())
    //    .observeOn(getForegroundScheduler())
    //    .subscribe(createPinEvent -> {
    //      if (createPinEvent.success()) {
    //        callback.onMasterPinCreateSuccess();
    //      } else {
    //        callback.onMasterPinCreateFailure();
    //      }
    //    }, throwable -> Timber.e(throwable, "on error create pin bus")));
    //
    //disposeOnStop(EventBus.get()
    //    .listen(ClearPinEvent.class)
    //    .subscribeOn(getBackgroundScheduler())
    //    .observeOn(getForegroundScheduler())
    //    .subscribe(clearPinEvent -> {
    //      if (clearPinEvent.success()) {
    //        callback.onMasterPinClearSuccess();
    //      } else {
    //        callback.onMasterPinClearFailure();
    //      }
    //    }, throwable -> Timber.e(throwable, "on error clear pin bus")));
  }

  /**
   * public
   */
  void populateList(boolean forceRefresh, @NonNull PopulateListCallback callback) {
    disposeOnStop(lockListInteractor.populateList(forceRefresh)
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .doAfterTerminate(callback::onListPopulated)
        .doOnSubscribe(disposable -> callback.onListPopulateBegin())
        .subscribe(callback::onEntryAddedToList, throwable -> {
          Timber.e(throwable, "populateList onError");
          callback.onListPopulateError();
        }));
  }

  /**
   * public
   */
  void setFABStateFromPreference(@NonNull FABStateCallback callback) {
    disposeOnStop(stateInteractor.isServiceEnabled()
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .subscribe(enabled -> {
          if (enabled) {
            callback.onSetFABStateEnabled();
          } else {
            callback.onSetFABStateDisabled();
          }
        }, throwable -> Timber.e(throwable, "onError")));
  }

  /**
   * public
   */
  void setSystemVisible() {
    lockListInteractor.setSystemVisible(true);
  }

  /**
   * public
   */
  void setSystemInvisible() {
    lockListInteractor.setSystemVisible(false);
  }

  /**
   * public
   */
  void setSystemVisibilityFromPreference(@NonNull SystemVisibilityCallback callback) {
    disposeOnStop(lockListInteractor.isSystemVisible()
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .subscribe(visible -> {
          if (visible) {
            callback.onSetSystemVisible();
          } else {
            callback.onSetSystemInvisible();
          }
        }, throwable -> Timber.e(throwable, "onError")));
  }

  /**
   * public
   */
  void showOnBoarding(@NonNull OnboardingCallback callback) {
    disposeOnStop(lockListInteractor.hasShownOnBoarding()
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .subscribe(onboard -> {
          if (onboard) {
            callback.onOnboardingComplete();
          } else {
            callback.onShowOnboarding();
          }
        }, throwable -> Timber.e(throwable, "onError")));
  }

  interface BusCallback {

    void onMasterPinCreateSuccess();

    void onMasterPinCreateFailure();

    void onMasterPinClearSuccess();

    void onMasterPinClearFailure();
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
