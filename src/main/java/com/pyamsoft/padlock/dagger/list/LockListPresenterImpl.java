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
import android.support.v4.util.Pair;
import com.pyamsoft.padlock.app.list.LockListPresenter;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroidrx.SubscriptionHelper;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
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

  @Inject LockListPresenterImpl(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(lockListInteractor, obsScheduler, subScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(populateListSubscription, systemVisibleSubscription,
        onboardSubscription, fabStateSubscription, databaseSubscription);
  }

  @Override public void populateList() {
    Timber.d("populateList");

    SubscriptionHelper.unsubscribe(populateListSubscription);
    populateListSubscription = lockListInteractor.getActiveApplications()
        .withLatestFrom(lockListInteractor.isSystemVisible(), (applicationInfo, systemVisible) -> {
          if (systemVisible) {
            // If system visible, we show all apps
            Timber.d("System visible: show %s", applicationInfo.packageName);
            return applicationInfo;
          } else {
            if (lockListInteractor.isSystemApplication(applicationInfo)) {
              // Application is system but system apps are hidden
              Timber.w("Hide system application: %s", applicationInfo.packageName);
              return null;
            } else {
              Timber.d("Visible: show %s", applicationInfo.packageName);
              return applicationInfo;
            }
          }
        })
        .filter(applicationInfo -> applicationInfo != null)
        .flatMap(
            applicationInfo -> lockListInteractor.getActivityListForApplication(applicationInfo)
                .toList()
                .map(activityList -> {
                  if (activityList.size() == 0) {
                    Timber.w("Exclude package %s because it has no activities",
                        applicationInfo.packageName);
                    return null;
                  } else {
                    return applicationInfo.packageName;
                  }
                }))
        .filter(s -> s != null)
        .withLatestFrom(lockListInteractor.getAppEntryList(), (packageName, padLockEntries) -> {
          PadLockEntry.AllEntries found = null;
          for (final PadLockEntry.AllEntries padLockEntry : padLockEntries) {
            if (padLockEntry.packageName().equals(packageName) && padLockEntry.activityName()
                .equals(PadLockEntry.PACKAGE_ACTIVITY_NAME)) {
              found = padLockEntry;
              break;
            }
          }

          Timber.d("New pair: %s %s", packageName, found != null);
          return new Pair<>(packageName, found != null);
        })
        .flatMap(pair -> lockListInteractor.createFromPackageInfo(pair.first, pair.second))
        .sorted((entry, entry2) -> entry.name().compareToIgnoreCase(entry2.name()))
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(appEntry -> getView(lockList -> lockList.onEntryAddedToList(appEntry)),
            throwable -> {
              Timber.e(throwable, "populateList onError");
              getView(LockList::onListPopulated);
            }, () -> {
              getView(LockList::onListPopulated);
              SubscriptionHelper.unsubscribe(populateListSubscription);
            });
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

  @Override public void clickPinFAB() {
    getView(lockList -> {
      if (PadLockService.isRunning()) {
        lockList.onCreatePinDialog();
      } else {
        lockList.onCreateAccessibilityDialog();
      }
    });
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
