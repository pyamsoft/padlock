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

package com.pyamsoft.padlock.settings;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class SettingsPreferencePresenter extends SchedulerPresenter<Presenter.Empty> {

  @SuppressWarnings("WeakerAccess") static final int CONFIRM_DATABASE = 0;
  @SuppressWarnings("WeakerAccess") static final int CONFIRM_ALL = 1;
  @SuppressWarnings("WeakerAccess") @NonNull final ApplicationInstallReceiver receiver;
  @NonNull private final SettingsPreferenceInteractor interactor;
  @NonNull private Subscription confirmedSubscription = Subscriptions.empty();
  @NonNull private Subscription applicationInstallSubscription = Subscriptions.empty();

  @Inject SettingsPreferencePresenter(@NonNull SettingsPreferenceInteractor interactor,
      @NonNull ApplicationInstallReceiver receiver, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    this.receiver = receiver;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    confirmedSubscription = SubscriptionHelper.unsubscribe(confirmedSubscription);
    applicationInstallSubscription = SubscriptionHelper.unsubscribe(applicationInstallSubscription);
  }

  public void requestClearAll(@NonNull RequestCallback callback) {
    callback.showConfirmDialog(CONFIRM_ALL);
  }

  public void requestClearDatabase(@NonNull RequestCallback callback) {
    callback.showConfirmDialog(CONFIRM_DATABASE);
  }

  public void setApplicationInstallReceiverState() {
    applicationInstallSubscription = SubscriptionHelper.unsubscribe(applicationInstallSubscription);
    applicationInstallSubscription = interactor.isInstallListenerEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(result -> {
          if (result) {
            receiver.register();
          } else {
            receiver.unregister();
          }
        }, throwable -> Timber.e(throwable, "onError setApplicationInstallReceiverState"));
  }

  public void processClearRequest(int type, @NonNull ClearCallback callback) {
    switch (type) {
      case CONFIRM_DATABASE:
        clearDatabase(callback);
        break;
      case CONFIRM_ALL:
        clearAll(callback);
        break;
      default:
        throw new IllegalStateException("Received invalid confirmation event type: " + type);
    }
  }

  private void clearAll(@NonNull ClearCallback callback) {
    confirmedSubscription = SubscriptionHelper.unsubscribe(confirmedSubscription);
    confirmedSubscription = interactor.clearAll()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> callback.onClearAll(), throwable -> Timber.e(throwable, "onError"));
  }

  private void clearDatabase(@NonNull ClearCallback callback) {
    confirmedSubscription = SubscriptionHelper.unsubscribe(confirmedSubscription);
    confirmedSubscription = interactor.clearDatabase()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(aBoolean -> callback.onClearDatabase(),
            throwable -> Timber.e(throwable, "onError"));
  }

  interface ClearCallback {
    void onClearAll();

    void onClearDatabase();
  }

  interface RequestCallback {

    void showConfirmDialog(int type);
  }
}
