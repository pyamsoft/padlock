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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.padlock.service.LockServiceStateInteractor;
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockListPresenterImpl extends SchedulerPresenter<LockListPresenter.LockList>
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
    super(obsScheduler, subScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(systemVisibleSubscription, onboardSubscription,
        fabStateSubscription, databaseSubscription, populateListSubscription);
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
    SubscriptionHelper.unsubscribe(populateListSubscription);
    populateListSubscription = lockListInteractor.populateList()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(appEntry -> {
          Timber.d("onNext!");
          getView(lockList -> lockList.onEntryAddedToList(appEntry));
        }, throwable -> {
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
            lockList.onSetFABStateEnabled();
          } else {
            lockList.onSetFABStateDisabled();
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
            lockList.onSetSystemVisible();
          } else {
            lockList.onSetSystemInvisible();
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
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> getView(lockList -> {
          if (onboard) {
            lockList.onOnboardingComplete();
          } else {
            lockList.onShowOnboarding();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          getView(LockList::onListPopulateError);
        }, () -> SubscriptionHelper.unsubscribe(onboardSubscription));
  }

  @Override
  public void modifyDatabaseEntry(boolean isChecked, int position, @NonNull String packageName,
      @Nullable String code, boolean system) {
    SubscriptionHelper.unsubscribe(databaseSubscription);

    // No whitelisting for modifications from the List
    databaseSubscription = lockListInteractor.modifySingleDatabaseEntry(isChecked, packageName,
        PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system, false, false)
        .subscribeOn(getSubscribeScheduler())
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
