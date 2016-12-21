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

package com.pyamsoft.padlockpresenter.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import com.pyamsoft.padlockmodel.AppEntry;
import com.pyamsoft.padlockmodel.sql.PadLockEntry;
import com.pyamsoft.padlockpresenter.service.LockServiceStateInteractor;
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

class LockListPresenterImpl extends LockCommonPresenterImpl<LockListPresenter.LockList>
    implements LockListPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription populateListSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription systemVisibleSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription onboardSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription fabStateSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription databaseSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") boolean refreshing;

  @Inject LockListPresenterImpl(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(lockListInteractor, obsScheduler, subScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
    refreshing = false;
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    SubscriptionHelper.unsubscribe(populateListSubscription);
    refreshing = false;
    clearList();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(systemVisibleSubscription, onboardSubscription,
        fabStateSubscription, databaseSubscription);
  }

  @Override public void clearList() {
    lockListInteractor.clearCache();
  }

  @Override
  public void updateCachedEntryLockState(@NonNull String name, @NonNull String packageName,
      boolean newLockState) {
    lockListInteractor.updateCacheEntry(name, packageName, newLockState);
  }

  @Override public void populateList() {
    if (refreshing) {
      Timber.w("Already refreshing, do nothing");
      return;
    }

    refreshing = true;
    Timber.d("populateList");

    final Observable<AppEntry> freshAppEntries = lockListInteractor.getActiveApplications()
        .withLatestFrom(lockListInteractor.isSystemVisible(), (applicationInfo, systemVisible) -> {
          if (systemVisible) {
            // If system visible, we show all apps
            return applicationInfo;
          } else {
            if (lockListInteractor.isSystemApplication(applicationInfo)) {
              // Application is system but system apps are hidden
              Timber.w("Hide system application: %s", applicationInfo.packageName);
              return null;
            } else {
              return applicationInfo;
            }
          }
        })
        .filter(applicationInfo -> applicationInfo != null)
        .flatMap(
            applicationInfo -> lockListInteractor.getActivityListForApplication(applicationInfo)
                .toList()
                .map(activityList -> {
                  if (activityList.isEmpty()) {
                    Timber.w("Exclude package %s because it has no activities",
                        applicationInfo.packageName);
                    return null;
                  } else {
                    return applicationInfo.packageName;
                  }
                }))
        .filter(s -> s != null)
        .toList()
        .withLatestFrom(lockListInteractor.getAppEntryList()
                .flatMap(Observable::from)
                .toSortedList((allEntries, allEntries2) -> allEntries.packageName()
                    .compareToIgnoreCase(allEntries2.packageName())),
            (packageNames, padLockEntries) -> {
              final List<Pair<String, Boolean>> lockPairs = new ArrayList<>();
              int start = 0;
              int end = packageNames.size() - 1;

              while (start <= end) {
                // Find entry to compare against
                final Pair<String, Boolean> entry1 =
                    findAppEntry(packageNames, padLockEntries, start);
                lockPairs.add(entry1);

                if (start != end) {
                  final Pair<String, Boolean> entry2 =
                      findAppEntry(packageNames, padLockEntries, end);
                  lockPairs.add(entry2);
                }

                ++start;
                --end;
              }

              return lockPairs;
            })
        .flatMap(Observable::from)
        .flatMap(pair -> lockListInteractor.createFromPackageInfo(pair.first, pair.second))
        .sorted((entry, entry2) -> entry.name().compareToIgnoreCase(entry2.name()))
        .map(appEntry -> {
          lockListInteractor.cacheEntry(appEntry);
          return appEntry;
        });

    final Observable<AppEntry> dataSource;
    if (lockListInteractor.isCacheEmpty()) {
      dataSource = freshAppEntries;
    } else {
      dataSource = lockListInteractor.getCachedEntries();
    }

    SubscriptionHelper.unsubscribe(populateListSubscription);
    populateListSubscription = dataSource.subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(appEntry -> getView(lockList -> lockList.onEntryAddedToList(appEntry)),
            throwable -> {
              Timber.e(throwable, "populateList onError");
              getView(LockList::onListPopulated);
            }, () -> {
              refreshing = false;
              getView(LockList::onListPopulated);
              SubscriptionHelper.unsubscribe(populateListSubscription);
            });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Pair<String, Boolean> findAppEntry(
      @NonNull List<String> packageNames, @NonNull List<PadLockEntry.AllEntries> padLockEntries,
      int index) {
    final String packageName = packageNames.get(index);
    final PadLockEntry.AllEntries foundEntry = findMatchingEntry(padLockEntries, packageName);
    return new Pair<>(packageName, foundEntry != null);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @Nullable
  PadLockEntry.AllEntries findMatchingEntry(@NonNull List<PadLockEntry.AllEntries> padLockEntries,
      @NonNull String packageName) {
    if (padLockEntries.isEmpty()) {
      return null;
    }

    // Select a pivot point
    final int middle = padLockEntries.size() / 2;
    final PadLockEntry.AllEntries pivotPoint = padLockEntries.get(middle);

    // Compare to pivot
    int start;
    int end;
    PadLockEntry.AllEntries foundEntry = null;
    if (pivotPoint.packageName().equals(packageName)) {
      // We are the pivot
      foundEntry = pivotPoint;
      start = 0;
      end = -1;
    } else if (packageName.compareToIgnoreCase(pivotPoint.packageName()) < 0) {
      //  We are before the pivot point
      start = 0;
      end = middle - 1;
    } else {
      // We are after the pivot point
      start = middle + 1;
      end = padLockEntries.size() - 1;
    }

    while (start <= end) {
      final PadLockEntry.AllEntries checkEntry1 = padLockEntries.get(start++);
      final PadLockEntry.AllEntries checkEntry2 = padLockEntries.get(end--);
      if (packageName.equals(checkEntry1.packageName())) {
        foundEntry = checkEntry1;
        break;
      } else if (packageName.equals(checkEntry2.packageName())) {
        foundEntry = checkEntry2;
        break;
      }
    }

    if (foundEntry != null) {
      padLockEntries.remove(foundEntry);
    }

    return foundEntry;
  }

  @Override public void setFABStateFromPreference() {
    SubscriptionHelper.unsubscribe(fabStateSubscription);
    fabStateSubscription = stateInteractor.isServiceEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(enabled -> getView(lockList -> {
          if (enabled) {
            lockList.setFABStateEnabled();
          } else {
            lockList.setFABStateDisabled();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          // TODO  different error
          getView(LockList::onListPopulateError);
        }, () -> SubscriptionHelper.unsubscribe(fabStateSubscription));
  }

  @Override public void setSystemVisible() {
    lockListInteractor.setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    lockListInteractor.setSystemVisible(false);
  }

  @Override public void setSystemVisibilityFromPreference() {
    SubscriptionHelper.unsubscribe(systemVisibleSubscription);
    systemVisibleSubscription = lockListInteractor.isSystemVisible()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(visible -> getView(lockList -> {
          if (visible) {
            lockList.setSystemVisible();
          } else {
            lockList.setSystemInvisible();
          }
        }), throwable -> {
          // TODO different error
          getView(LockList::onListPopulateError);
        }, () -> SubscriptionHelper.unsubscribe(systemVisibleSubscription));
  }

  @Override public void clickPinFABServiceIdle() {
    getView(LockList::onCreateAccessibilityDialog);
  }

  @Override public void clickPinFABServiceRunning() {
    getView(LockList::onCreatePinDialog);
  }

  @Override public void showOnBoarding() {
    SubscriptionHelper.unsubscribe(onboardSubscription);
    onboardSubscription = lockListInteractor.hasShownOnBoarding()
        .delay(1, TimeUnit.SECONDS)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> getView(lockList -> {
          if (!onboard) {
            lockList.showOnBoarding();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          getView(LockList::onListPopulateError);
        }, () -> SubscriptionHelper.unsubscribe(onboardSubscription));
  }

  @Override public void setOnBoard() {
    lockListInteractor.setShownOnBoarding();
  }

  @Override
  public void modifyDatabaseEntry(boolean isChecked, int position, @NonNull String packageName,
      @Nullable String code, boolean system) {
    SubscriptionHelper.unsubscribe(databaseSubscription);

    // No whitelisting for modifications from the List
    databaseSubscription =
        modifySingleDatabaseEntry(isChecked, packageName, PadLockEntry.PACKAGE_ACTIVITY_NAME, code,
            system, false, false).subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(lockState -> {
              switch (lockState) {
                case DEFAULT:
                  getView(lockList -> lockList.onDatabaseEntryDeleted(position));
                  break;
                case LOCKED:
                  getView(lockList -> lockList.onDatabaseEntryCreated(position));
                  break;
                default:
                  throw new RuntimeException("Whitelist results are not handled");
              }
            }, throwable -> {
              Timber.e(throwable, "onError modifyDatabaseEntry");
              getView(lockList -> lockList.onDatabaseEntryError(position));
            }, () -> SubscriptionHelper.unsubscribe(databaseSubscription));
  }
}
