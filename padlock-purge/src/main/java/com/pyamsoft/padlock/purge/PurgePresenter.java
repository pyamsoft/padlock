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

package com.pyamsoft.padlock.purge;

import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class PurgePresenter extends SchedulerPresenter<Presenter.Empty> {

  @SuppressWarnings("WeakerAccess") @NonNull final PurgeInteractor interactor;
  @NonNull private final CompositeSubscription compositeSubscription;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription retrievalSubscription =
      Subscriptions.empty();

  @Inject PurgePresenter(@NonNull PurgeInteractor interactor, @NonNull Scheduler observeScheduler,
      @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
    compositeSubscription = new CompositeSubscription();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    compositeSubscription.clear();
    SubscriptionHelper.unsubscribe(retrievalSubscription);
  }

  public void retrieveStaleApplications(@NonNull RetrievalCallback callback, boolean forceRefresh) {
    SubscriptionHelper.unsubscribe(retrievalSubscription);
    retrievalSubscription = interactor.populateList(forceRefresh)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .doAfterTerminate(callback::onRetrievalComplete)
        .subscribe(callback::onStaleApplicationRetrieved,
            throwable -> Timber.e(throwable, "onError retrieveStaleApplications"),
            () -> SubscriptionHelper.unsubscribe(retrievalSubscription));
  }

  public void deleteStale(@NonNull String packageName, @NonNull DeleteCallback callback) {
    final Subscription subscription = interactor.deleteEntry(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(deleteResult -> {
          Timber.d("Delete result :%d", deleteResult);
          if (deleteResult > 0) {
            onDeleteSuccess(packageName, callback);
          }
        }, throwable -> {
          Timber.e(throwable, "onError deleteStale");
        });
    compositeSubscription.add(subscription);
  }

  @SuppressWarnings("WeakerAccess") void onDeleteSuccess(@NonNull String packageName,
      @NonNull DeleteCallback callback) {
    callback.onDeleted(packageName);
  }

  interface RetrievalCallback {

    void onStaleApplicationRetrieved(@NonNull String name);

    void onRetrievalComplete();
  }

  interface DeleteCallback {

    void onDeleted(@NonNull String packageName);
  }
}
