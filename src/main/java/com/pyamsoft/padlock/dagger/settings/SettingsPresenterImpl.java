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
import com.pyamsoft.padlock.app.lockscreen.LockScreenInteractor;
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.padlock.app.settings.SettingsInteractor;
import com.pyamsoft.padlock.app.settings.SettingsPresenter;
import com.pyamsoft.padlock.app.settings.SettingsView;
import com.pyamsoft.padlock.model.event.ConfirmationEvent;
import com.pyamsoft.pydroid.base.PresenterImplBase;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

final class SettingsPresenterImpl extends PresenterImplBase<SettingsView>
    implements SettingsPresenter {

  @NonNull private final LockScreenInteractor lockScreenInteractor;
  @NonNull private final SettingsInteractor settingsInteractor;

  private Subscription confirmDialogBusSubscription;
  private Subscription ignorePeriodSubscription;
  private Subscription timeoutSubscription;
  private Subscription confirmDialogSubscription;

  @Inject public SettingsPresenterImpl(@NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull final SettingsInteractor settingsInteractor) {
    this.lockScreenInteractor = lockScreenInteractor;
    this.settingsInteractor = settingsInteractor;
  }

  @Override public void unbind() {
    super.unbind();

    unsubscribeIgnorePeriod();
    unsubscribeConfirmDialog();
    unsubscribeTimeout();
    unregisterFromConfirmDialogBus();
  }

  private void unsubscribeConfirmDialog() {
    if (confirmDialogSubscription != null) {
      if (confirmDialogSubscription.isUnsubscribed()) {
        confirmDialogSubscription.unsubscribe();
      }
      confirmDialogSubscription = null;
    }
  }

  private void unsubscribeIgnorePeriod() {
    if (ignorePeriodSubscription != null) {
      if (ignorePeriodSubscription.isUnsubscribed()) {
        ignorePeriodSubscription.unsubscribe();
      }

      ignorePeriodSubscription = null;
    }
  }

  private void unsubscribeTimeout() {
    if (timeoutSubscription != null) {
      if (timeoutSubscription.isUnsubscribed()) {
        timeoutSubscription.unsubscribe();
      }

      timeoutSubscription = null;
    }
  }

  @Override public void setIgnorePeriodFromPreference() {
    unsubscribeIgnorePeriod();
    ignorePeriodSubscription =
        Observable.defer(() -> Observable.just(lockScreenInteractor.getDefaultIgnoreTime()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(time -> {
              final SettingsView settingsView = get();
              if (time == PadLockPreferences.PERIOD_FIVE) {
                settingsView.setIgnorePeriodFive();
              } else if (time == PadLockPreferences.PERIOD_TEN) {
                settingsView.setIgnorePeriodTen();
              } else if (time == PadLockPreferences.PERIOD_THIRTY) {
                settingsView.setIgnorePeriodThirty();
              } else {
                settingsView.setIgnorePeriodNone();
              }
            }, throwable -> {
              Timber.e(throwable, "setIgnorePeriodFromPreference onError");
            });
  }

  @Override public void setIgnorePeriodNone() {
    lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_NONE);
  }

  @Override public void setIgnorePeriodFive() {
    lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_FIVE);
  }

  @Override public void setIgnorePeriodTen() {
    lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_TEN);
  }

  @Override public void setIgnorePeriodThirty() {
    lockScreenInteractor.setDefaultIgnoreTime(PadLockPreferences.PERIOD_THIRTY);
  }

  @Override public void setTimeoutPeriodFromPreference() {
    unsubscribeTimeout();
    timeoutSubscription =
        Observable.defer(() -> Observable.just(lockScreenInteractor.getTimeoutPeriod()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(time -> {
              final SettingsView settingsView = get();
              if (time == PadLockPreferences.PERIOD_ONE) {
                settingsView.setTimeoutPeriodOne();
              } else if (time == PadLockPreferences.PERIOD_FIVE) {
                settingsView.setTimeoutPeriodFive();
              } else if (time == PadLockPreferences.PERIOD_TEN) {
                settingsView.setTimeoutPeriodTen();
              } else {
                settingsView.setTimeoutPeriodNone();
              }
            }, throwable -> {
              Timber.e(throwable, "setTimeoutPeriodFromPreference onError");
            });
  }

  @Override public void setTimeoutPeriodNone() {
    lockScreenInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_NONE);
  }

  @Override public void setTimeoutPeriodOne() {
    lockScreenInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_ONE);
  }

  @Override public void setTimeoutPeriodFive() {
    lockScreenInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_FIVE);
  }

  @Override public void setTimeoutPeriodTen() {
    lockScreenInteractor.setTimeoutPeriod(PadLockPreferences.PERIOD_TEN);
  }

  private Observable<Boolean> clearDatabase() {
    Timber.d("Clear database");
    return Observable.defer(() -> {
      settingsInteractor.clearDatabase();
      return Observable.just(true);
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  private Observable<Boolean> clearAll() {
    Timber.d("Clear all settings");
    return Observable.defer(() -> {
      settingsInteractor.clearAll();
      return Observable.just(true);
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  @Override public void unregisterFromConfirmDialogBus() {
    if (confirmDialogBusSubscription != null) {
      if (!confirmDialogBusSubscription.isUnsubscribed()) {
        confirmDialogBusSubscription.unsubscribe();
      }
      confirmDialogBusSubscription = null;
    }
  }

  @Override public void registerOnConfirmDialogBus() {
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

                }, throwable -> Timber.e(throwable, "ConfirmationDialogBus in clearDatabase onError"), () -> {
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

                }, throwable -> Timber.e(throwable, "ConfirmationDialogBus in clearAll onError"), () -> {
                  Timber.d("ConfirmationDialogBus in clearAll onComplete");
                  ConfirmationDialog.ConfirmationDialogBus.get()
                      .post(ConfirmationEvent.builder(confirmationEvent).complete(true).build());
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
