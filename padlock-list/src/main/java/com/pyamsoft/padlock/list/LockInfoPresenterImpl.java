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

package com.pyamsoft.padlock.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockInfoPresenterImpl extends SchedulerPresenter<LockInfoPresenter.LockInfoView>
    implements LockInfoPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final LockInfoInteractor lockInfoInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription populateListSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription databaseSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription onboardSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") boolean refreshing;

  @Inject LockInfoPresenterImpl(final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("obs") Scheduler obsScheduler,
      final @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.lockInfoInteractor = lockInfoInteractor;
    refreshing = false;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(databaseSubscription, onboardSubscription);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    SubscriptionHelper.unsubscribe(populateListSubscription);
    clearList();
    refreshing = false;
  }

  @Override
  public void updateCachedEntryLockState(@NonNull String name, @NonNull LockState lockState) {
    lockInfoInteractor.updateCacheEntry(name, lockState);
  }

  @Override public void clearList() {
    lockInfoInteractor.clearCache();
  }

  @Override public void populateList(@NonNull String packageName) {
    if (refreshing) {
      Timber.w("Already refreshing, do nothing");
      return;
    }

    refreshing = true;
    Timber.d("Populate list");

    final Observable<ActivityEntry> freshData = lockInfoInteractor.getPackageActivities(packageName)
        .zipWith(lockInfoInteractor.getLockedActivityEntries(packageName),
            (activityNames, padLockEntries) -> {
              // Sort here to avoid stream break
              // If the list is empty, the old flatMap call can hang, causing a list loading error
              // Sort here where we are guaranteed a list of some kind
              Collections.sort(padLockEntries,
                  (o1, o2) -> o1.activityName().compareToIgnoreCase(o2.activityName()));

              final List<ActivityEntry> activityEntries = new ArrayList<>();

              int start = 0;
              int end = activityNames.size() - 1;

              while (start <= end) {
                // Find entry to compare against
                final ActivityEntry entry1 =
                    findActivityEntry(activityNames, padLockEntries, start);
                activityEntries.add(entry1);

                if (start != end) {
                  final ActivityEntry entry2 =
                      findActivityEntry(activityNames, padLockEntries, end);
                  activityEntries.add(entry2);
                }

                ++start;
                --end;
              }

              return activityEntries;
            })
        .flatMap(Observable::from)
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
          lockInfoInteractor.cacheEntry(activityEntry);
          return activityEntry;
        });

    final Observable<ActivityEntry> dataSource;
    if (lockInfoInteractor.isCacheEmpty()) {
      dataSource = freshData;
    } else {
      dataSource = lockInfoInteractor.getCachedEntries();
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

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull ActivityEntry findActivityEntry(
      @NonNull List<String> activityNames,
      @NonNull List<PadLockEntry.WithPackageName> padLockEntries, int index) {
    final String activityName = activityNames.get(index);
    final PadLockEntry.WithPackageName foundEntry = findMatchingEntry(padLockEntries, activityName);
    return createActivityEntry(activityName, foundEntry);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @Nullable
  PadLockEntry.WithPackageName findMatchingEntry(
      @NonNull List<PadLockEntry.WithPackageName> padLockEntries, @NonNull String activityName) {
    if (padLockEntries.isEmpty()) {
      return null;
    }

    // Select a pivot point
    final int middle = padLockEntries.size() / 2;
    final PadLockEntry.WithPackageName pivotPoint = padLockEntries.get(middle);

    // Compare to pivot
    int start;
    int end;
    PadLockEntry.WithPackageName foundEntry = null;
    if (pivotPoint.activityName().equals(activityName)) {
      // We are the pivot
      foundEntry = pivotPoint;
      start = 0;
      end = -1;
    } else if (activityName.compareToIgnoreCase(pivotPoint.activityName()) < 0) {
      //  We are before the pivot point
      start = 0;
      end = middle - 1;
    } else {
      // We are after the pivot point
      start = middle + 1;
      end = padLockEntries.size() - 1;
    }

    while (start <= end) {
      final PadLockEntry.WithPackageName checkEntry1 = padLockEntries.get(start++);
      final PadLockEntry.WithPackageName checkEntry2 = padLockEntries.get(end--);
      if (activityName.equals(checkEntry1.activityName())) {
        foundEntry = checkEntry1;
        break;
      } else if (activityName.equals(checkEntry2.activityName())) {
        foundEntry = checkEntry2;
        break;
      }
    }

    if (foundEntry != null) {
      padLockEntries.remove(foundEntry);
    }

    return foundEntry;
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull ActivityEntry createActivityEntry(
      @NonNull String name, @Nullable PadLockEntry.WithPackageName foundEntry) {
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

    return ActivityEntry.builder().name(name).lockState(state).build();
  }

  @Override
  public void modifyDatabaseEntry(boolean isDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist,
      boolean forceDelete) {
    SubscriptionHelper.unsubscribe(databaseSubscription);
    databaseSubscription =
        lockInfoInteractor.modifySingleDatabaseEntry(isDefault, packageName, activityName, code,
            system, whitelist, forceDelete)
            .flatMap(lockState -> {
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
