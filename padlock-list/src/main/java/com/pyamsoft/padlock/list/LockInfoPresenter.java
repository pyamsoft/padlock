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
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockInfoPresenter extends SchedulerPresenter<Presenter.Empty> {

  @SuppressWarnings("WeakerAccess") @NonNull final LockInfoInteractor lockInfoInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription populateListSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription databaseSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription onboardSubscription =
      Subscriptions.empty();

  @Inject LockInfoPresenter(final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("obs") Scheduler obsScheduler,
      final @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.lockInfoInteractor = lockInfoInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(databaseSubscription, onboardSubscription,
        populateListSubscription);
  }

  public void updateCachedEntryLockState(@NonNull String packageName, @NonNull String name,
      @NonNull LockState lockState) {
    lockInfoInteractor.updateCacheEntry(packageName, name, lockState);
  }

  public void clearList(@NonNull ClearCallback callback) {
    lockInfoInteractor.clearCache();
    callback.onListCleared();
  }

  public void populateList(@NonNull String packageName, @NonNull PopulateListCallback callback) {
    SubscriptionHelper.unsubscribe(populateListSubscription);
    populateListSubscription = lockInfoInteractor.populateList(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::onEntryAddedToList, throwable -> {
          Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
          callback.onListPopulateError();
        }, () -> {
          callback.onListPopulated();
          SubscriptionHelper.unsubscribe(populateListSubscription);
        });
  }

  public void modifyDatabaseEntry(boolean isNotDefault, int position, @NonNull String packageName,
      @NonNull String activityName, @SuppressWarnings("SameParameterValue") @Nullable String code,
      boolean system, boolean whitelist, boolean forceDelete,
      @NonNull ModifyDatabaseCallback callback) {
    SubscriptionHelper.unsubscribe(databaseSubscription);
    databaseSubscription =
        lockInfoInteractor.modifySingleDatabaseEntry(isNotDefault, packageName, activityName, code,
            system, whitelist, forceDelete)
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(lockState -> {
              switch (lockState) {
                case DEFAULT:
                  callback.onDatabaseEntryDeleted(position);
                  break;
                case WHITELISTED:
                  callback.onDatabaseEntryWhitelisted(position);
                  break;
                case LOCKED:
                  callback.onDatabaseEntryCreated(position);
                  break;
                default:
                  throw new IllegalStateException("Unsupported lock state: " + lockState);
              }
            }, throwable -> {
              Timber.e(throwable, "onError modifyDatabaseEntry");
              callback.onDatabaseEntryError(position);
            }, () -> SubscriptionHelper.unsubscribe(databaseSubscription));
  }

  public void showOnBoarding(@NonNull OnBoardingCallback callback) {
    SubscriptionHelper.unsubscribe(onboardSubscription);
    onboardSubscription = lockInfoInteractor.hasShownOnBoarding()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> {
          if (onboard) {
            callback.onOnboardingComplete();
          } else {
            callback.onShowOnboarding();
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
        }, () -> SubscriptionHelper.unsubscribe(onboardSubscription));
  }

  public interface ModifyDatabaseCallback extends LockDatabaseErrorView, LockDatabaseWhitelistView {

  }

  public interface OnBoardingCallback {

    void onShowOnboarding();

    void onOnboardingComplete();
  }

  public interface PopulateListCallback extends LockCommon {

    void onEntryAddedToList(@NonNull ActivityEntry entry);
  }
}
