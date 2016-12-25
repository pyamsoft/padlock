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

package com.pyamsoft.presenter.purge;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import com.pyamsoft.pydroidrx.SubscriptionHelper;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import rx.Observable;
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

  @Override protected void onDestroy() {
    super.onDestroy();
    unsubStaleDeletes();
    SubscriptionHelper.unsubscribe(retrievalSubscription);
  }

  private void unsubStaleDeletes() {
    if (compositeSubscription.hasSubscriptions()) {
      compositeSubscription.clear();
    }
  }

  @Override public void clearList() {
    interactor.clearCache();
  }

  @Override public void retrieveStaleApplications() {
    if (refreshing) {
      Timber.w("Already refreshing, do nothing");
      return;
    }

    refreshing = true;
    final Observable<String> freshData = interactor.getAppEntryList()
        .zipWith(interactor.getActiveApplicationPackageNames().toList(),
            (allEntries, packageNames) -> {
              final Set<String> stalePackageNames = new HashSet<>();
              if (allEntries.isEmpty()) {
                Timber.e("Database does not have any AppEntry items");
                return stalePackageNames;
              }

              // Loop through all the package names that we are aware of on the device
              final Set<PadLockEntry.AllEntries> foundLocations = new HashSet<>();
              for (final String packageName : packageNames) {
                foundLocations.clear();

                //noinspection Convert2streamapi
                for (final PadLockEntry.AllEntries entry : allEntries) {
                  // If an entry is found in the database remove it
                  if (entry.packageName().equals(packageName)) {
                    foundLocations.add(entry);
                  }
                }

                allEntries.removeAll(foundLocations);
              }

              // The remaining entries in the database are stale
              //noinspection Convert2streamapi
              for (final PadLockEntry.AllEntries entry : allEntries) {
                stalePackageNames.add(entry.packageName());
              }

              return stalePackageNames;
            })
        .flatMap(Observable::from)
        .sorted(String::compareToIgnoreCase)
        .map(packageName -> {
          interactor.cacheEntry(packageName);
          return packageName;
        });

    final Observable<String> dataSource;
    if (interactor.isCacheEmpty()) {
      dataSource = freshData;
    } else {
      dataSource = interactor.getCachedEntries();
    }

    SubscriptionHelper.unsubscribe(retrievalSubscription);
    retrievalSubscription = dataSource.subscribeOn(getSubscribeScheduler())
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
    getView(view -> view.onDeleted(packageName));
    interactor.removeFromCache(packageName);
  }
}
