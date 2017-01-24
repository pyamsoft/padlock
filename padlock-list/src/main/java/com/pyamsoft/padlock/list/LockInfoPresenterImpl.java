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
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
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

  @Inject LockInfoPresenterImpl(final @NonNull LockInfoInteractor lockInfoInteractor,
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

  @Override
  public void updateCachedEntryLockState(@NonNull String packageName, @NonNull String name,
      @NonNull LockState lockState) {
    lockInfoInteractor.updateCacheEntry(packageName, name, lockState);
  }

  @Override public void clearList() {
    lockInfoInteractor.clearCache();
  }

  @Override public void populateList(@NonNull String packageName) {
    SubscriptionHelper.unsubscribe(populateListSubscription);
    populateListSubscription = lockInfoInteractor.populateList(packageName)
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
          if (onboard) {
            lockList.onOnboardingComplete();
          } else {
            lockList.onShowOnboarding();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          getView(LockInfoView::onListPopulateError);
        }, () -> SubscriptionHelper.unsubscribe(onboardSubscription));
  }
}
