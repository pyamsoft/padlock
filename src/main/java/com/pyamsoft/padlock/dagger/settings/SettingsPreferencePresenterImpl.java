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
import com.pyamsoft.padlock.app.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.app.settings.SettingsPreferencePresenter;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class SettingsPreferencePresenterImpl
    extends SchedulerPresenter<SettingsPreferencePresenter.SettingsPreferenceView>
    implements SettingsPreferencePresenter {

  @SuppressWarnings("WeakerAccess") static final int CONFIRM_DATABASE = 0;
  @SuppressWarnings("WeakerAccess") static final int CONFIRM_ALL = 1;
  @SuppressWarnings("WeakerAccess") @NonNull final ApplicationInstallReceiver receiver;
  @NonNull private final SettingsPreferenceInteractor interactor;
  @NonNull private Subscription confirmedSubscription = Subscriptions.empty();
  @NonNull private Subscription applicationInstallSubscription = Subscriptions.empty();

  @Inject SettingsPreferencePresenterImpl(@NonNull SettingsPreferenceInteractor interactor,
      @NonNull ApplicationInstallReceiver receiver, @NonNull Scheduler obsScheduler,
      @NonNull Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    this.receiver = receiver;
  }

  @Override protected void onBind() {
    super.onBind();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubscribeConfirm();
    unsubApplicationInstallReceiver();
  }

  @Override public void requestClearAll() {
    getView(settingsPreferenceView -> settingsPreferenceView.showConfirmDialog(CONFIRM_ALL));
  }

  @Override public void requestClearDatabase() {
    getView(settingsPreferenceView -> settingsPreferenceView.showConfirmDialog(CONFIRM_DATABASE));
  }

  @Override public void setApplicationInstallReceiverState() {
    applicationInstallSubscription = interactor.isInstallListenerEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(result -> {
              if (result) {
                receiver.register();
              } else {
                receiver.unregister();
              }
            }, throwable -> Timber.e(throwable, "onError setApplicationInstallReceiverState"),
            this::unsubApplicationInstallReceiver);
  }

  @SuppressWarnings("WeakerAccess") void unsubApplicationInstallReceiver() {
    if (!applicationInstallSubscription.isUnsubscribed()) {
      applicationInstallSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeConfirm() {
    if (!confirmedSubscription.isUnsubscribed()) {
      confirmedSubscription.unsubscribe();
    }
  }

  @Override public void processClearRequest(int type) {
    switch (type) {
      case CONFIRM_DATABASE:
        clearDatabase();
        break;
      case CONFIRM_ALL:
        clearAll();
        break;
      default:
        throw new IllegalStateException("Received invalid confirmation event type: " + type);
    }
  }

  @SuppressWarnings("WeakerAccess") void clearAll() {
    unsubscribeConfirm();
    confirmedSubscription = interactor.clearAll()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> getView(SettingsPreferenceView::onClearAll),
            throwable -> Timber.e(throwable, "onError"), this::unsubscribeConfirm);
  }

  @SuppressWarnings("WeakerAccess") void clearDatabase() {
    unsubscribeConfirm();
    confirmedSubscription = interactor.clearDatabase()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> getView(SettingsPreferenceView::onClearDatabase),
            throwable -> Timber.e(throwable, "onError"), this::unsubscribeConfirm);
  }
}
