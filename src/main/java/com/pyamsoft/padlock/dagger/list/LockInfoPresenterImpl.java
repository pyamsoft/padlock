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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.list.LockInfoPresenter;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroidrx.SubscriptionHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockInfoPresenterImpl extends LockCommonPresenterImpl<LockInfoPresenter.LockInfoView>
    implements LockInfoPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final AppIconLoaderPresenter<LockInfoView> iconLoader;
  @SuppressWarnings("WeakerAccess") @NonNull final LockInfoInteractor lockInfoInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull final List<ActivityEntry> activityEntryCache;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription populateListSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription databaseSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription onboardSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") boolean refreshing;

  @Inject LockInfoPresenterImpl(@NonNull AppIconLoaderPresenter<LockInfoView> iconLoader,
      final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("obs") Scheduler obsScheduler,
      final @NonNull @Named("io") Scheduler subScheduler) {
    super(lockInfoInteractor, obsScheduler, subScheduler);
    this.iconLoader = iconLoader;
    this.lockInfoInteractor = lockInfoInteractor;
    activityEntryCache = new ArrayList<>();
    refreshing = false;
  }

  @Override protected void onBind() {
    super.onBind();
    getView(iconLoader::bindView);
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    iconLoader.unbindView();
    SubscriptionHelper.unsubscribe(databaseSubscription, onboardSubscription);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    iconLoader.destroy();
    SubscriptionHelper.unsubscribe(populateListSubscription);
    clearList();
    refreshing = false;
  }

  @Override
  public void updateCachedEntryLockState(@NonNull String name, @NonNull LockState lockState) {
    final int size = activityEntryCache.size();
    for (int i = 0; i < size; ++i) {
      final ActivityEntry activityEntry = activityEntryCache.get(i);
      if (activityEntry.name().equals(name)) {
        Timber.d("Update cached entry: %s", name);
        activityEntryCache.set(i,
            ActivityEntry.builder(activityEntry).lockState(lockState).build());
      }
    }
  }

  @Override public void clearList() {
    Timber.w("Clear activity entry cache");
    activityEntryCache.clear();
  }

  @Override public void populateList(@NonNull String packageName) {
    if (refreshing) {
      Timber.w("Already refreshing, do nothing");
      return;
    }

    refreshing = true;
    Timber.d("Populate list");

    final Observable<ActivityEntry> freshData = lockInfoInteractor.getPackageActivities(packageName)
        .withLatestFrom(lockInfoInteractor.getActivityEntries(packageName),
            (activityName, padLockEntries) -> {
              PadLockEntry.WithPackageName foundEntry = null;
              for (int i = 0; i < padLockEntries.size(); ++i) {
                final PadLockEntry.WithPackageName entry = padLockEntries.get(i);
                if (entry.activityName().equals(activityName)) {
                  foundEntry = entry;
                  break;
                }
              }

              final LockState state;
              if (foundEntry == null) {
                state = LockState.DEFAULT;
              } else {
                if (foundEntry.whitelist()) {
                  state = LockState.WHITELISTED;
                } else {
                  state = LockState.LOCKED;
                }
              }

              return ActivityEntry.builder().name(activityName).lockState(state).build();
            })
        .sorted((activityEntry, activityEntry2) -> {
          // Package names are all the same
          final String entry1Name = activityEntry.name();
          final String entry2Name = activityEntry2.name();

          // Calculate if the starting X characters in the activity name is the exact package name
          boolean activity1Package = false;
          if (entry1Name.startsWith(packageName)) {
            final String strippedPackageName = entry1Name.replace(packageName, "");
            if (strippedPackageName.charAt(0) == '.') {
              activity1Package = true;
            }
          }

          boolean activity2Package = false;
          if (entry2Name.startsWith(packageName)) {
            final String strippedPackageName = entry2Name.replace(packageName, "");
            if (strippedPackageName.charAt(0) == '.') {
              activity2Package = true;
            }
          }
          if (activity1Package && activity2Package) {
            return entry1Name.compareToIgnoreCase(entry2Name);
          } else if (activity1Package) {
            return -1;
          } else if (activity2Package) {
            return 1;
          } else {
            return entry1Name.compareToIgnoreCase(entry2Name);
          }
        })
        .map(activityEntry -> {
          activityEntryCache.add(activityEntry);
          return activityEntry;
        });

    final Observable<ActivityEntry> dataSource;
    if (activityEntryCache.isEmpty()) {
      dataSource = freshData;
    } else {
      dataSource = Observable.defer(() -> Observable.from(activityEntryCache));
    }

    SubscriptionHelper.unsubscribe(populateListSubscription);
    // Search the list of activities in the package name for any which are locked
    populateListSubscription = dataSource.subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(activityEntry -> getView(
            lockInfoView -> lockInfoView.onEntryAddedToList(activityEntry)), throwable -> {
          Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
          getView(LockInfoView::onListPopulateError);
        }, () -> {
          refreshing = false;
          getView(LockInfoView::onListPopulated);
          SubscriptionHelper.unsubscribe(populateListSubscription);
        });
  }

  @Override
  public void modifyDatabaseEntry(boolean isDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist,
      boolean forceDelete) {
    SubscriptionHelper.unsubscribe(databaseSubscription);
    databaseSubscription =
        modifySingleDatabaseEntry(isDefault, packageName, activityName, code, system, whitelist,
            forceDelete).flatMap(lockState -> {
          final Observable<LockState> resultState;
          if (lockState == LockState.NONE) {
            Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated");
            resultState =
                lockInfoInteractor.updateExistingEntry(packageName, activityName, whitelist);
          } else {
            resultState = Observable.just(lockState);
          }
          return resultState;
        })
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(lockState -> {
              switch (lockState) {
                case DEFAULT:
                  getView(lockInfoView -> lockInfoView.onDatabaseEntryDeleted(position));
                  break;
                case WHITELISTED:
                  getView(lockInfoView -> lockInfoView.onDatabaseEntryWhitelisted(position));
                  break;
                case LOCKED:
                  getView(lockInfoView -> lockInfoView.onDatabaseEntryCreated(position));
                  break;
                default:
                  throw new IllegalStateException("Unsupported lock state: " + lockState);
              }
            }, throwable -> {
              Timber.e(throwable, "onError modifyDatabaseEntry");
              getView(lockInfoView -> lockInfoView.onDatabaseEntryError(position));
            }, () -> SubscriptionHelper.unsubscribe(databaseSubscription));
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    iconLoader.loadApplicationIcon(packageName);
  }

  @Override public void showOnBoarding() {
    SubscriptionHelper.unsubscribe(onboardSubscription);
    onboardSubscription = lockInfoInteractor.hasShownOnBoarding()
        .delay(1, TimeUnit.SECONDS)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> getView(lockList -> {
          if (!onboard) {
            lockList.showOnBoarding();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          getView(LockInfoView::onListPopulateError);
        }, () -> SubscriptionHelper.unsubscribe(onboardSubscription));
  }

  @Override public void setOnBoard() {
    lockInfoInteractor.setShownOnBoarding();
  }
}
