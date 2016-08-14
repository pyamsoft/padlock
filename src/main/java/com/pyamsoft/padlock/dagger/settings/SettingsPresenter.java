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
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.padlock.dagger.base.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class SettingsPresenter extends SchedulerPresenter<SettingsPresenter.SettingsView> {

  public static final int CONFIRM_DATABASE = 0;
  public static final int CONFIRM_ALL = 1;
  @NonNull private final SettingsInteractor interactor;
  @NonNull private Subscription confirmBusSubscription = Subscriptions.empty();
  @NonNull private Subscription confirmedSubscription = Subscriptions.empty();

  @Inject SettingsPresenter(@NonNull SettingsInteractor interactor,
      @NonNull @Named("io") Scheduler ioScheduler,
      @NonNull @Named("main") Scheduler mainScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
  }

  @Override protected void onResume(@NonNull SettingsView view) {
    super.onResume(view);
    registerOnConfirmEventBus();
  }

  @Override protected void onPause(@NonNull SettingsView view) {
    super.onPause(view);
    unregisterFromConfirmEventBus();
  }

  @Override protected void onUnbind(@NonNull SettingsView view) {
    super.onUnbind(view);
    unsubscribeConfirm();
  }

  public void requestClearAll() {
    getView().showConfirmDialog(CONFIRM_ALL);
  }

  public void requestClearDatabase() {
    getView().showConfirmDialog(CONFIRM_DATABASE);
  }

  void unsubscribeConfirm() {
    if (!confirmedSubscription.isUnsubscribed()) {
      confirmedSubscription.unsubscribe();
    }
  }

  void unregisterFromConfirmEventBus() {
    if (!confirmBusSubscription.isUnsubscribed()) {
      confirmBusSubscription.unsubscribe();
    }
  }

  void registerOnConfirmEventBus() {
    unregisterFromConfirmEventBus();
    confirmBusSubscription = ConfirmationDialog.Bus.get()
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

  void clearAll() {
    unsubscribeConfirm();
    confirmedSubscription = interactor.clearAll()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> {
          getView().onClearAll();
        }, throwable -> {
          Timber.e(throwable, "onError");
        }, this::unsubscribeConfirm);
  }

  void clearDatabase() {
    unsubscribeConfirm();
    confirmedSubscription = interactor.clearDatabase()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> {
          getView().onClearDatabase();
        }, throwable -> {
          Timber.e(throwable, "onError");
        }, this::unsubscribeConfirm);
  }

  public interface SettingsView {

    void showConfirmDialog(int type);

    void onClearAll();

    void onClearDatabase();
  }
}
