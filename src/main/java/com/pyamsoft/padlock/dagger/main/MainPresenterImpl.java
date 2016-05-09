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

package com.pyamsoft.padlock.dagger.main;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.main.AgreeTermsDialog;
import com.pyamsoft.padlock.app.main.MainInteractor;
import com.pyamsoft.padlock.app.main.MainPresenter;
import com.pyamsoft.padlock.app.main.MainView;
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class MainPresenterImpl extends PresenterImpl<MainView> implements MainPresenter {

  @NonNull private final MainInteractor interactor;

  @NonNull private Subscription confirmDialogBusSubscription = Subscriptions.empty();
  @NonNull private Subscription agreeTermsBusSubscription = Subscriptions.empty();
  @NonNull private Subscription agreeTermsSubscription = Subscriptions.empty();

  @Inject public MainPresenterImpl(@NonNull final MainInteractor interactor) {
    this.interactor = interactor;
  }

  @Override public void stop() {
    super.stop();
    unregisterFromConfirmDialogBus();
    unregisterFromAgreeTermsBus();
    unsubAgreeTermsSubscription();
  }

  @Override public void showTermsDialog() {
    unsubAgreeTermsSubscription();
    agreeTermsSubscription = interactor.hasAgreed()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(agreed -> {
          if (!agreed) {
            get().showUsageTermsDialog();
          }
        }, throwable -> {
          // TODO handle error
          Timber.e(throwable, "onError");
        });
  }

  private void unsubAgreeTermsSubscription() {
    if (!agreeTermsSubscription.isUnsubscribed()) {
      agreeTermsSubscription.unsubscribe();
    }
  }

  @Override public void registerOnAgreeTermsBus() {
    unregisterFromAgreeTermsBus();
    agreeTermsBusSubscription =
        AgreeTermsDialog.AgreeTermsBus.get().register().subscribe(agreeTermsEvent -> {
          final MainView mainView = get();
          if (agreeTermsEvent.agreed()) {
            unsubAgreeTermsSubscription();
            agreeTermsSubscription = interactor.setAgreed()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
          } else {
            mainView.onDidNotAgreeToTerms();
          }
        }, throwable -> {
          Timber.e(throwable, "AgreeTermsBus onError");
        });
  }

  @Override public void unregisterFromAgreeTermsBus() {
    if (!agreeTermsBusSubscription.isUnsubscribed()) {
      agreeTermsBusSubscription.unsubscribe();
    }
  }

  @Override public void registerOnConfirmDialogBus() {
    unregisterFromConfirmDialogBus();
    confirmDialogBusSubscription =
        ConfirmationDialog.ConfirmationDialogBus.get().register().subscribe(confirmationEvent -> {
          if (confirmationEvent.type() == 1 && confirmationEvent.complete()) {
            Timber.d("received completed clearAll event. Kill Process");
            android.os.Process.killProcess(android.os.Process.myPid());
          }
        }, throwable -> {
          Timber.e(throwable, "ConfirmationDialogBus onError");
        });
  }

  @Override public void unregisterFromConfirmDialogBus() {
    if (!confirmDialogBusSubscription.isUnsubscribed()) {
      confirmDialogBusSubscription.unsubscribe();
    }
  }
}
