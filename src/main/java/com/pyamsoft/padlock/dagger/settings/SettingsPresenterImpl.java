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
import com.pyamsoft.padlock.app.settings.SettingsPresenter;
import com.pyamsoft.padlock.model.event.ConfirmationEvent;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class SettingsPresenterImpl extends PresenterImpl<SettingsPresenter.SettingsView>
    implements SettingsPresenter {

  @NonNull private final SettingsInteractor settingsInteractor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription confirmDialogBusSubscription = Subscriptions.empty();
  @NonNull private Subscription confirmDialogSubscription = Subscriptions.empty();

  @Inject public SettingsPresenterImpl(@NonNull final SettingsInteractor settingsInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.settingsInteractor = settingsInteractor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    unsubscribeConfirmDialog();
  }

  @Override public void onResume() {
    super.onResume();
    registerOnConfirmDialogBus();
  }

  @Override public void onPause() {
    super.onPause();
    unregisterFromConfirmDialogBus();
  }

  private void unsubscribeConfirmDialog() {
    if (confirmDialogSubscription.isUnsubscribed()) {
      confirmDialogSubscription.unsubscribe();
    }
  }

  private Observable<Boolean> clearDatabase() {
    return settingsInteractor.clearDatabase().subscribeOn(ioScheduler).observeOn(mainScheduler);
  }

  private Observable<Boolean> clearAll() {
    return settingsInteractor.clearAll().subscribeOn(ioScheduler).observeOn(mainScheduler);
  }

  private void unregisterFromConfirmDialogBus() {
    if (!confirmDialogBusSubscription.isUnsubscribed()) {
      confirmDialogBusSubscription.unsubscribe();
    }
  }

  private void registerOnConfirmDialogBus() {
    unregisterFromConfirmDialogBus();
    confirmDialogBusSubscription =
        ConfirmationDialog.ConfirmationDialogBus.get().register().subscribe(confirmationEvent -> {
          Timber.d("Received confirmation event!");
          // KLUDGE nested subscriptions are ugly
          switch (confirmationEvent.type()) {
            case 0:
              if (!confirmationEvent.complete()) {
                unsubscribeConfirmDialog();
                Timber.d("Received database cleared confirmation event, clear Database");
                confirmDialogSubscription = clearDatabase().subscribe(aBoolean -> {

                }, throwable -> Timber.e(throwable,
                    "ConfirmationDialogBus in clearDatabase onError"), () -> {
                  Timber.d("ConfirmationDialogBus in clearDatabase onComplete");
                  ConfirmationDialog.ConfirmationDialogBus.get()
                      .post(ConfirmationEvent.builder(confirmationEvent).complete(true).build());
                });
              }
              break;
            case 1:
              if (!confirmationEvent.complete()) {
                unsubscribeConfirmDialog();
                Timber.d("Received all cleared confirmation event, clear All");
                confirmDialogSubscription = clearAll().subscribe(aBoolean -> {

                    }, throwable -> Timber.e(throwable, "ConfirmationDialogBus in clearAll onError"),
                    () -> {
                      Timber.d("ConfirmationDialogBus in clearAll onComplete");
                      ConfirmationDialog.ConfirmationDialogBus.get()
                          .post(
                              ConfirmationEvent.builder(confirmationEvent).complete(true).build());
                    });
              }
              break;
            default:
          }
        }, throwable -> {
          Timber.e(throwable, "ConfirmationDialogBus onError");
        });
  }
}
