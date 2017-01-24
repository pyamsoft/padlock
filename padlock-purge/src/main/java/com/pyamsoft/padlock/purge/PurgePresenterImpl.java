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
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class PurgePresenterImpl extends SchedulerPresenter<PurgePresenter.View> implements PurgePresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final PurgeInteractor interactor;
  @NonNull private final CompositeSubscription compositeSubscription;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription retrievalSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") boolean refreshing;

  @Inject PurgePresenterImpl(@NonNull PurgeInteractor interactor,
      @NonNull Scheduler observeScheduler, @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
    compositeSubscription = new CompositeSubscription();
    refreshing = false;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    compositeSubscription.clear();
    SubscriptionHelper.unsubscribe(retrievalSubscription);
  }

  @Override public void clearList() {
    interactor.clearCache();
  }

  @Override public void retrieveStaleApplications() {
    SubscriptionHelper.unsubscribe(retrievalSubscription);
    retrievalSubscription = interactor.populateList()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(s -> getView(view -> view.onStaleApplicationRetrieved(s)), throwable -> {
          Timber.e(throwable, "onError retrieveStaleApplications");
          getView(View::onRetrievalComplete);
        }, () -> {
          refreshing = false;
          SubscriptionHelper.unsubscribe(retrievalSubscription);
          getView(View::onRetrievalComplete);
        });
  }

  @Override public void deleteStale(@NonNull String packageName) {
    final Subscription subscription = interactor.deleteEntry(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(deleteResult -> {
          Timber.d("Delete result :%d", deleteResult);
          if (deleteResult > 0) {
            onDeleteSuccess(packageName);
          }
        }, throwable -> {
          Timber.e(throwable, "onError deleteStale");
        });
    compositeSubscription.add(subscription);
  }

  @SuppressWarnings("WeakerAccess") void onDeleteSuccess(@NonNull String packageName) {
    interactor.removeFromCache(packageName);
    getView(view -> view.onDeleted(packageName));
  }
}
