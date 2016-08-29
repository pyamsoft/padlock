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

package com.pyamsoft.padlock.dagger.settings;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.pyamsoft.padlock.app.bus.ConfirmDialogBus;
import com.pyamsoft.padlock.app.settings.SettingsPreferencePresenter;
import com.pyamsoft.pydroid.base.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class SettingsPreferencePresenterImpl
    extends SchedulerPresenter<SettingsPreferencePresenter.SettingsPreferenceView>
    implements SettingsPreferencePresenter {

  @SuppressWarnings("WeakerAccess") static final int CONFIRM_DATABASE = 0;
  @SuppressWarnings("WeakerAccess") static final int CONFIRM_ALL = 1;
  @NonNull private final SettingsPreferenceInteractor interactor;
  @NonNull private Subscription confirmBusSubscription = Subscriptions.empty();
  @NonNull private Subscription confirmedSubscription = Subscriptions.empty();

  @Inject SettingsPreferencePresenterImpl(@NonNull SettingsPreferenceInteractor interactor,
      @NonNull @Named("io") Scheduler ioScheduler,
      @NonNull @Named("main") Scheduler mainScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
  }

  @Override protected void onBind(@NonNull SettingsPreferenceView view) {
    registerOnConfirmEventBus();
    super.onBind(view);
  }

  @Override protected void onUnbind(@NonNull SettingsPreferenceView view) {
    super.onUnbind(view);
    unregisterFromConfirmEventBus();
    unsubscribeConfirm();
  }

  @Override public void requestClearAll() {
    getView().showConfirmDialog(CONFIRM_ALL);
  }

  @Override public void requestClearDatabase() {
    getView().showConfirmDialog(CONFIRM_DATABASE);
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeConfirm() {
    if (!confirmedSubscription.isUnsubscribed()) {
      confirmedSubscription.unsubscribe();
    }
  }

  private void unregisterFromConfirmEventBus() {
    if (!confirmBusSubscription.isUnsubscribed()) {
      confirmBusSubscription.unsubscribe();
    }
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess") void registerOnConfirmEventBus() {
    unregisterFromConfirmEventBus();
    confirmBusSubscription = ConfirmDialogBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(confirmationEvent -> {
          switch (confirmationEvent.type()) {
            case CONFIRM_DATABASE:
              clearDatabase();
              break;
            case CONFIRM_ALL:
              clearAll();
              break;
            default:
              throw new IllegalStateException(
                  "Received invalid confirmation event type: " + confirmationEvent.type());
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
        });
  }

  @SuppressWarnings("WeakerAccess") void clearAll() {
    unsubscribeConfirm();
    confirmedSubscription = interactor.clearAll()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> getView().onClearAll(), throwable -> Timber.e(throwable, "onError"),
            this::unsubscribeConfirm);
  }

  @SuppressWarnings("WeakerAccess") void clearDatabase() {
    unsubscribeConfirm();
    confirmedSubscription = interactor.clearDatabase()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> getView().onClearDatabase(),
            throwable -> Timber.e(throwable, "onError"), this::unsubscribeConfirm);
  }
}
