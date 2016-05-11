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
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.padlock.app.settings.SettingsPresenter;
import com.pyamsoft.padlock.dagger.lockscreen.LockScreenInteractor;
import com.pyamsoft.padlock.model.event.ConfirmationEvent;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class SettingsPresenterImpl extends PresenterImpl<SettingsPresenter.SettingsView>
    implements SettingsPresenter {

  @NonNull private final LockScreenInteractor lockScreenInteractor;
  @NonNull private final SettingsInteractor settingsInteractor;

  @NonNull private Subscription confirmDialogBusSubscription = Subscriptions.empty();
  @NonNull private Subscription ignorePeriodSubscription = Subscriptions.empty();
  @NonNull private Subscription timeoutSubscription = Subscriptions.empty();
  @NonNull private Subscription confirmDialogSubscription = Subscriptions.empty();

  @Inject public SettingsPresenterImpl(@NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull final SettingsInteractor settingsInteractor) {
    this.lockScreenInteractor = lockScreenInteractor;
    this.settingsInteractor = settingsInteractor;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    unsubscribeIgnorePeriod();
    unsubscribeConfirmDialog();
    unsubscribeTimeout();
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

  private void unsubscribeIgnorePeriod() {
    if (ignorePeriodSubscription.isUnsubscribed()) {
      ignorePeriodSubscription.unsubscribe();
    }
  }

  private void unsubscribeTimeout() {
    if (timeoutSubscription.isUnsubscribed()) {
      timeoutSubscription.unsubscribe();
    }
  }

  @Override public void setIgnorePeriodFromPreference() {
    unsubscribeIgnorePeriod();
    ignorePeriodSubscription = lockScreenInteractor.getDefaultIgnoreTime()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(time -> {
          final SettingsView settingsView = getView();
          if (settingsView != null) {
            if (time == PadLockPreferences.PERIOD_FIVE) {
              settingsView.setIgnorePeriodFive();
            } else if (time == PadLockPreferences.PERIOD_TEN) {
              settingsView.setIgnorePeriodTen();
            } else if (time == PadLockPreferences.PERIOD_THIRTY) {
              settingsView.setIgnorePeriodThirty();
            } else {
              settingsView.setIgnorePeriodNone();
            }
          }
        }, throwable -> {
          Timber.e(throwable, "setIgnorePeriodFromPreference onError");
        });
  }

  @Override public void setIgnorePeriodNone() {
    unsubscribeIgnorePeriod();
    ignorePeriodSubscription =
        lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_NONE)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe();
  }

  @Override public void setIgnorePeriodFive() {
    unsubscribeIgnorePeriod();
    ignorePeriodSubscription =
        lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_FIVE)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe();
  }

  @Override public void setIgnorePeriodTen() {
    unsubscribeIgnorePeriod();
    ignorePeriodSubscription =
        lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_TEN)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe();
  }

  @Override public void setIgnorePeriodThirty() {
    unsubscribeIgnorePeriod();
    ignorePeriodSubscription =
        lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_THIRTY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe();
  }

  @Override public void setTimeoutPeriodFromPreference() {
    unsubscribeTimeout();
    timeoutSubscription = settingsInteractor.getTimeoutPeriod()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(time -> {
          final SettingsView settingsView = getView();
          if (settingsView != null) {
            if (time == PadLockPreferences.PERIOD_ONE) {
              settingsView.setTimeoutPeriodOne();
            } else if (time == PadLockPreferences.PERIOD_FIVE) {
              settingsView.setTimeoutPeriodFive();
            } else if (time == PadLockPreferences.PERIOD_TEN) {
              settingsView.setTimeoutPeriodTen();
            } else {
              settingsView.setTimeoutPeriodNone();
            }
          }
        }, throwable -> {
          Timber.e(throwable, "setTimeoutPeriodFromPreference onError");
        });
  }

  @Override public void setTimeoutPeriodNone() {
    unsubscribeTimeout();
    settingsInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_NONE)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  @Override public void setTimeoutPeriodOne() {
    unsubscribeTimeout();
    settingsInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_ONE)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  @Override public void setTimeoutPeriodFive() {
    unsubscribeTimeout();
    settingsInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_FIVE)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  @Override public void setTimeoutPeriodTen() {
    unsubscribeTimeout();
    settingsInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_TEN)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  private Observable<Boolean> clearDatabase() {
    return settingsInteractor.clearDatabase()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  private Observable<Boolean> clearAll() {
    return settingsInteractor.clearAll()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
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
