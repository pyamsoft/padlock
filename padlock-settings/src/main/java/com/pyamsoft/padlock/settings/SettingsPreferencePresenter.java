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

package com.pyamsoft.padlock.settings;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class SettingsPreferencePresenter extends SchedulerPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final ApplicationInstallReceiver receiver;
  @SuppressWarnings("WeakerAccess") @NonNull final SettingsPreferenceInteractor interactor;

  @Inject SettingsPreferencePresenter(@NonNull SettingsPreferenceInteractor interactor,
      @NonNull ApplicationInstallReceiver receiver, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    this.receiver = receiver;
  }

  /**
   * public
   */
  void setApplicationInstallReceiverState() {
    disposeOnStop(interactor.isInstallListenerEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(result -> {
          if (result) {
            receiver.register();
          } else {
            receiver.unregister();
          }
        }, throwable -> Timber.e(throwable, "onError setApplicationInstallReceiverState")));
  }

  /**
   * public
   */
  void registerOnBus(@NonNull ClearCallback callback) {
    disposeOnStop(EventBus.get().listen(ConfirmEvent.class).flatMapSingle(confirmEvent -> {
      Single<ConfirmEvent.Type> result;
      switch (confirmEvent.type()) {
        case DATABASE:
          result = interactor.clearDatabase().map(ignore -> ConfirmEvent.Type.DATABASE);
          break;
        case ALL:
          result = interactor.clearAll().map(ignore -> ConfirmEvent.Type.ALL);
          break;
        default:
          throw new IllegalStateException(
              "Received invalid confirmation event type: " + confirmEvent.type());
      }

      return result;
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(type -> {
      switch (type) {
        case DATABASE:
          callback.onClearDatabase();
          break;
        case ALL:
          callback.onClearAll();
          break;
        default:
          throw new IllegalStateException("Received invalid confirmation event type: " + type);
      }
    }, throwable -> Timber.e(throwable, "onError clear bus")));
  }

  /**
   * public
   */
  void checkLockType(@NonNull LockTypeCallback callback) {
    disposeOnStop(interactor.hasExistingMasterPassword()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .doAfterTerminate(callback::onEnd)
        .doOnSubscribe(disposable -> callback.onBegin())
        .subscribe(hasMasterPin -> {
          if (hasMasterPin) {
            callback.onLockTypeChangePrevented();
          } else {
            callback.onLockTypeChangeAccepted();
          }
        }, throwable -> {
          Timber.e(throwable, "on error lock type change");
          callback.onLockTypeChangeError(throwable);
        }));
  }

  interface LockTypeCallback {

    void onBegin();

    void onLockTypeChangeAccepted();

    void onLockTypeChangePrevented();

    void onLockTypeChangeError(@NonNull Throwable throwable);

    void onEnd();
  }

  interface ClearCallback {
    void onClearAll();

    void onClearDatabase();
  }
}
