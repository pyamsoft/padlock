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
import com.pyamsoft.pydroid.base.PresenterImplBase;
import javax.inject.Inject;
import rx.Subscription;
import timber.log.Timber;

final class MainPresenterImpl extends PresenterImplBase<MainView> implements MainPresenter {

  @NonNull private final MainInteractor interactor;

  private Subscription confirmDialogBusSubscription;
  private Subscription agreeTermsBusSubscription;

  @Inject public MainPresenterImpl(@NonNull final MainInteractor interactor) {
    this.interactor = interactor;
  }

  @Override public void unbind() {
    super.unbind();
    unregisterFromConfirmDialogBus();
    unregisterFromAgreeTermsBus();
  }

  @Override public void showTermsDialog() {
    final MainView mainView = get();
    if (!interactor.hasAgreed()) {
      mainView.showUsageTermsDialog();
    }
  }

  @Override public void registerOnAgreeTermsBus() {
    unregisterFromAgreeTermsBus();
    agreeTermsBusSubscription =
        AgreeTermsDialog.AgreeTermsBus.get().register().subscribe(agreeTermsEvent -> {
          final MainView mainView = get();
          if (agreeTermsEvent.agreed()) {
            interactor.setAgreed();
          } else {
            mainView.onDidNotAgreeToTerms();
          }
        }, throwable -> {
          Timber.e(throwable, "AgreeTermsBus onError");
        });
  }

  @Override public void unregisterFromAgreeTermsBus() {
    if (agreeTermsBusSubscription != null) {
      if (!agreeTermsBusSubscription.isUnsubscribed()) {
        agreeTermsBusSubscription.unsubscribe();
      }
      agreeTermsBusSubscription = null;
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
    if (confirmDialogBusSubscription != null) {
      if (!confirmDialogBusSubscription.isUnsubscribed()) {
        confirmDialogBusSubscription.unsubscribe();
      }
      confirmDialogBusSubscription = null;
    }
  }
}
