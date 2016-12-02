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

import android.support.annotation.CheckResult;
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
  @SuppressWarnings("WeakerAccess") @NonNull Subscription populateListSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription allInDBSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription databaseSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription onboardSubscription =
      Subscriptions.empty();

  @Inject LockInfoPresenterImpl(@NonNull AppIconLoaderPresenter<LockInfoView> iconLoader,
      final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("obs") Scheduler obsScheduler,
      final @NonNull @Named("io") Scheduler subScheduler) {
    super(lockInfoInteractor, obsScheduler, subScheduler);
    this.iconLoader = iconLoader;
    this.lockInfoInteractor = lockInfoInteractor;
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @Nullable
  PadLockEntry.WithPackageName findEntryInActivities(
      @NonNull List<PadLockEntry.WithPackageName> padLockEntries, @NonNull String name) {
    PadLockEntry.WithPackageName found = null;
    for (final PadLockEntry.WithPackageName padLockEntry : padLockEntries) {
      if (padLockEntry.activityName().equals(name)) {
        found = padLockEntry;
        break;
      }
    }

    return found;
  }

  @Override protected void onBind() {
    super.onBind();
    getView(iconLoader::bindView);
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    iconLoader.unbindView();
    SubscriptionHelper.unsubscribe(databaseSubscription, populateListSubscription,
        allInDBSubscription, onboardSubscription);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    iconLoader.destroy();
  }

  @Override public void populateList(@NonNull String packageName) {
    SubscriptionHelper.unsubscribe(populateListSubscription);

    // Filter out the lockscreen and crashlog entries
    final Observable<List<String>> activityInfoObservable =
        lockInfoInteractor.getPackageActivities(packageName).toList();

    // Zip together the lists into a list of ActivityEntry objects
    populateListSubscription =
        Observable.zip(lockInfoInteractor.getActivityEntries(packageName), activityInfoObservable,
            (padLockEntries, activityInfos) -> {
              final List<ActivityEntry> entries = new ArrayList<>();
              for (final String name : activityInfos) {
                final PadLockEntry.WithPackageName foundEntry =
                    findEntryInActivities(padLockEntries, name);

                if (foundEntry != null) {
                  padLockEntries.remove(foundEntry);
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

                entries.add(ActivityEntry.builder().lockState(state).name(name).build());
              }

              return entries;
            })
            .flatMap(Observable::from)
            .filter(activityEntry -> activityEntry != null)
            .sorted((activityEntry, activityEntry2) -> {
              final boolean activity1Package = activityEntry.name().startsWith(packageName);
              final boolean activity2Package = activityEntry2.name().startsWith(packageName);
              if (activity1Package && activity2Package) {
                return activityEntry.name().compareToIgnoreCase(activityEntry2.name());
              } else if (activity1Package) {
                return -1;
              } else if (activity2Package) {
                return 1;
              } else {
                return activityEntry.name().compareToIgnoreCase(activityEntry2.name());
              }
            })
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(activityEntry -> getView(
                lockInfoView -> lockInfoView.onEntryAddedToList(activityEntry)), throwable -> {
              Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
              getView(LockInfoView::onListPopulateError);
            }, () -> {
              getView(LockInfoView::onListPopulated);
              SubscriptionHelper.unsubscribe(populateListSubscription);
            });
  }

  @Override public void setToggleAllState(@NonNull String packageName) {
    SubscriptionHelper.unsubscribe(allInDBSubscription);

    // Filter out the lockscreen and crashlog entries
    final Observable<List<String>> activityInfoObservable =
        lockInfoInteractor.getPackageActivities(packageName).toList();

    // Zip together the lists into a list of ActivityEntry objects
    allInDBSubscription =
        Observable.zip(lockInfoInteractor.getActivityEntries(packageName), activityInfoObservable,
            (padLockEntries, activityInfos) -> {
              int count = 0;
              // KLUDGE super ugly.
              Timber.d("Search set for locked activities");
              for (final String name : activityInfos) {
                final PadLockEntry.WithPackageName found =
                    findEntryInActivities(padLockEntries, name);

                // If we found an entry, increment count
                if (found != null) {
                  ++count;
                }
              }

              return count == activityInfos.size();
            })
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(allIn -> getView(lockInfoView -> {
              if (allIn) {
                lockInfoView.enableToggleAll();
              } else {
                lockInfoView.disableToggleAll();
              }
            }), throwable -> {
              Timber.e(throwable, "onError");
              // TODO maybe different error
              getView(LockInfoView::onListPopulateError);
            }, () -> SubscriptionHelper.unsubscribe(allInDBSubscription));
  }

  @Override
  public void modifyDatabaseEntry(boolean isDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist,
      boolean forceDelete) {
    SubscriptionHelper.unsubscribe(databaseSubscription);
    databaseSubscription =
        modifySingleDatabaseEntry(isDefault, packageName, activityName, code, system, whitelist,
            forceDelete).subscribeOn(getSubscribeScheduler())
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
              }
            }, throwable -> {
              Timber.e(throwable, "onError modifyDatabaseEntry");
              getView(lockInfoView -> lockInfoView.onDatabaseEntryError(position));
            }, () -> SubscriptionHelper.unsubscribe(databaseSubscription));
  }

  @Override public void modifyDatabaseGroup(boolean allCreate, @NonNull String packageName,
      @Nullable String code, boolean system) {
    final Observable<String> packageActivitiesObservable =
        lockInfoInteractor.getPackageActivities(packageName);
    final Observable<Boolean> modifyDatabaseObservable;
    if (allCreate) {
      modifyDatabaseObservable = packageActivitiesObservable.flatMap(
          activityName -> lockInfoInteractor.insertDatabaseGroup(packageName, activityName, code,
              system));
    } else {
      modifyDatabaseObservable = packageActivitiesObservable.flatMap(
          activityName -> lockInfoInteractor.deleteDatabaseGroup(packageName, activityName));
    }

    SubscriptionHelper.unsubscribe(databaseSubscription);
    databaseSubscription = modifyDatabaseObservable.toList().map(result -> {
      Timber.d(
          "To prevent a bunch of events from occurring for each list entry, we flatten to just a single result");
      // We return the original request
      return allCreate;
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(created -> {
      if (created) {
        getView(lockInfoView -> lockInfoView.onDatabaseEntryCreated(GROUP_POSITION));
      } else {
        getView(lockInfoView -> lockInfoView.onDatabaseEntryDeleted(GROUP_POSITION));
      }
    }, throwable -> {
      Timber.e(throwable, "onError modifyDatabaseGroup");
      getView(lockInfoView -> lockInfoView.onDatabaseEntryError(GROUP_POSITION));
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
