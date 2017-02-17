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
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.padlock.service.LockServiceStateInteractor;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockListPresenter extends SchedulerPresenter<Presenter.Empty> {

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

  @Inject LockListPresenter(final @NonNull LockListInteractor lockListInteractor,
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

  public void clearList(@NonNull ClearCallback callback) {
    lockListInteractor.clearCache();
    callback.onListCleared();
  }

  public void updateCachedEntryLockState(@NonNull String name, @NonNull String packageName,
      boolean newLockState) {
    lockListInteractor.updateCacheEntry(name, packageName, newLockState);
  }

  public void populateList(@NonNull PopulateListCallback callback) {
    SubscriptionHelper.unsubscribe(populateListSubscription);
    populateListSubscription = lockListInteractor.populateList()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::onEntryAddedToList, throwable -> {
          Timber.e(throwable, "populateList onError");
          callback.onListPopulateError();
          callback.onListPopulated();
        }, () -> {
          callback.onListPopulated();
          SubscriptionHelper.unsubscribe(populateListSubscription);
        });
  }

  public void setFABStateFromPreference(@NonNull FABStateCallback callback) {
    SubscriptionHelper.unsubscribe(fabStateSubscription);
    fabStateSubscription = stateInteractor.isServiceEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(enabled -> {
              if (enabled) {
                callback.onSetFABStateEnabled();
              } else {
                callback.onSetFABStateDisabled();
              }
            }, throwable -> Timber.e(throwable, "onError"),
            () -> SubscriptionHelper.unsubscribe(fabStateSubscription));
  }

  public void setSystemVisible() {
    lockListInteractor.setSystemVisible(true);
  }

  public void setSystemInvisible() {
    lockListInteractor.setSystemVisible(false);
  }

  public void setSystemVisibilityFromPreference(@NonNull SystemVisibilityCallback callback) {
    SubscriptionHelper.unsubscribe(systemVisibleSubscription);
    systemVisibleSubscription = lockListInteractor.isSystemVisible()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(visible -> {
              if (visible) {
                callback.onSetSystemVisible();
              } else {
                callback.onSetSystemInvisible();
              }
            }, throwable -> Timber.e(throwable, "onError"),
            () -> SubscriptionHelper.unsubscribe(systemVisibleSubscription));
  }

  public void clickPinFABServiceIdle(@NonNull ShowAccessibilityCallback callback) {
    callback.onCreateAccessibilityDialog();
  }

  public void clickPinFABServiceRunning(@NonNull ShowPinCallback callback) {
    callback.onCreatePinDialog();
  }

  public void showOnBoarding(@NonNull OnboardingCallback callback) {
    SubscriptionHelper.unsubscribe(onboardSubscription);
    onboardSubscription = lockListInteractor.hasShownOnBoarding()
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

  public void modifyDatabaseEntry(boolean isChecked, int position, @NonNull String packageName,
      @SuppressWarnings("SameParameterValue") @Nullable String code, boolean system,
      @NonNull DatabaseCallback callback) {
    // No whitelisting for modifications from the List
    SubscriptionHelper.unsubscribe(databaseSubscription);
    databaseSubscription = lockListInteractor.modifySingleDatabaseEntry(isChecked, packageName,
        PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system, false, false)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockState -> {
          switch (lockState) {
            case DEFAULT:
              callback.onDatabaseEntryDeleted(position);
              break;
            case LOCKED:
              callback.onDatabaseEntryCreated(position);
              break;
            default:
              throw new RuntimeException("Whitelist results are not handled");
          }
        }, throwable -> {
          Timber.e(throwable, "onError modifyDatabaseEntry");
          callback.onDatabaseEntryError(position);
        }, () -> SubscriptionHelper.unsubscribe(databaseSubscription));
  }

  interface OnboardingCallback {

    void onShowOnboarding();

    void onOnboardingComplete();
  }

  interface FABStateCallback {

    void onSetFABStateEnabled();

    void onSetFABStateDisabled();
  }

  interface SystemVisibilityCallback {

    void onSetSystemVisible();

    void onSetSystemInvisible();
  }

  interface PopulateListCallback extends LockCommon {

    void onEntryAddedToList(@NonNull AppEntry entry);
  }

  interface DatabaseCallback extends LockDatabaseErrorView {
  }

  interface ShowPinCallback {

    void onCreatePinDialog();
  }

  interface ShowAccessibilityCallback {

    void onCreateAccessibilityDialog();
  }
}
