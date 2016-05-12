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
import com.pyamsoft.padlock.app.main.MainPresenter;
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class MainPresenterImpl extends PresenterImpl<MainPresenter.MainView>
    implements MainPresenter {

  @NonNull private final MainInteractor interactor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription confirmDialogBusSubscription = Subscriptions.empty();
  @NonNull private Subscription agreeTermsBusSubscription = Subscriptions.empty();
  @NonNull private Subscription agreeTermsSubscription = Subscriptions.empty();

  @Inject public MainPresenterImpl(@NonNull final MainInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.interactor = interactor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubAgreeTermsSubscription();
  }

  @Override public void onResume() {
    super.onResume();
    registerOnConfirmDialogBus();
    registerOnAgreeTermsBus();
  }

  @Override public void onPause() {
    super.onPause();
    unregisterFromConfirmDialogBus();
    unregisterFromAgreeTermsBus();
  }

  @Override public void showTermsDialog() {
    unsubAgreeTermsSubscription();
    agreeTermsSubscription = interactor.hasAgreed()
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe(agreed -> {
          final MainView mainView = getView();
          if (mainView != null) {
            if (!agreed) {
              mainView.showUsageTermsDialog();
            }
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

  private void registerOnAgreeTermsBus() {
    unregisterFromAgreeTermsBus();
    agreeTermsBusSubscription =
        AgreeTermsDialog.AgreeTermsBus.get().register().subscribe(agreeTermsEvent -> {
          if (agreeTermsEvent.agreed()) {
            unsubAgreeTermsSubscription();
            agreeTermsSubscription = interactor.setAgreed()
                .subscribeOn(ioScheduler)
                .observeOn(mainScheduler)
                .subscribe();
          } else {
            final MainView mainView = getView();
            if (mainView != null) {
              mainView.onDidNotAgreeToTerms();
            }
          }
        }, throwable -> {
          Timber.e(throwable, "AgreeTermsBus onError");
        });
  }

  private void unregisterFromAgreeTermsBus() {
    if (!agreeTermsBusSubscription.isUnsubscribed()) {
      agreeTermsBusSubscription.unsubscribe();
    }
  }

  private void registerOnConfirmDialogBus() {
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

  private void unregisterFromConfirmDialogBus() {
    if (!confirmDialogBusSubscription.isUnsubscribed()) {
      confirmDialogBusSubscription.unsubscribe();
    }
  }
}
