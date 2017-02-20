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

package com.pyamsoft.padlock.lock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.pydroid.helper.SubscriptionHelper;
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockScreenPresenter extends SchedulerPresenter<Presenter.Empty> {

  @NonNull private final LockScreenInteractor interactor;
  @NonNull private Subscription postUnlockSubscription = Subscriptions.empty();
  @NonNull private Subscription displayNameSubscription = Subscriptions.empty();
  @NonNull private Subscription ignoreTimeSubscription = Subscriptions.empty();
  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();
  @NonNull private Subscription hintSubscription = Subscriptions.empty();
  @NonNull private Subscription typeSubscription = Subscriptions.empty();

  @Inject LockScreenPresenter(@NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = lockScreenInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    postUnlockSubscription = SubscriptionHelper.unsubscribe(postUnlockSubscription);
    displayNameSubscription = SubscriptionHelper.unsubscribe(displayNameSubscription);
    ignoreTimeSubscription = SubscriptionHelper.unsubscribe(ignoreTimeSubscription);
    unlockSubscription = SubscriptionHelper.unsubscribe(unlockSubscription);
    lockSubscription = SubscriptionHelper.unsubscribe(lockSubscription);
    hintSubscription = SubscriptionHelper.unsubscribe(hintSubscription);
  }

  public void resetFailCount() {
    interactor.resetFailCount();
  }

  public void initializeLockScreenType(@NonNull LockScreenTypeCallback callback) {
    typeSubscription = SubscriptionHelper.unsubscribe(typeSubscription);
    typeSubscription = interactor.getLockScreenType()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockScreenType -> {
          switch (lockScreenType) {
            case TYPE_PATTERN:
              callback.onTypePattern();
              break;
            case TYPE_TEXT:
              callback.onTypeText();
              break;
            default:
              throw new IllegalStateException("Invalid lock screen type: " + lockScreenType);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
        });
  }

  public void displayLockedHint(@NonNull LockHintCallback callback) {
    hintSubscription = SubscriptionHelper.unsubscribe(hintSubscription);
    hintSubscription = interactor.getHint()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::setDisplayHint,
            throwable -> Timber.e(throwable, "onError displayLockedHint"));
  }

  public void createWithDefaultIgnoreTime(@NonNull IgnoreTimeCallback callback) {
    ignoreTimeSubscription = SubscriptionHelper.unsubscribe(ignoreTimeSubscription);
    ignoreTimeSubscription = interactor.getDefaultIgnoreTime()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::onInitializeWithIgnoreTime,
            throwable -> Timber.e(throwable, "onError createWithDefaultIgnoreTime"));
  }

  public void lockEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull LockCallback callback) {
    lockSubscription = SubscriptionHelper.unsubscribe(lockSubscription);
    lockSubscription = interactor.incrementAndGetFailCount(packageName, activityName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockTime -> {
          Timber.d("Received lock entry result");
          callback.onLocked(lockTime);
        }, throwable -> {
          Timber.e(throwable, "lockEntry onError");
          callback.onLockedError();
        });
  }

  public void submit(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, @NonNull String currentAttempt,
      @NonNull LockSubmitCallback callback) {
    unlockSubscription = SubscriptionHelper.unsubscribe(unlockSubscription);
    unlockSubscription =
        interactor.submitPin(packageName, activityName, lockCode, lockUntilTime, currentAttempt)
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(unlocked -> {
              Timber.d("Received unlock entry result");
              if (unlocked) {
                callback.onSubmitSuccess();
              } else {
                callback.onSubmitFailure();
              }
            }, throwable -> {
              Timber.e(throwable, "unlockEntry onError");
              callback.onSubmitError();
            });
  }

  public void loadDisplayNameFromPackage(@NonNull String packageName,
      @NonNull DisplayNameLoadCallback callback) {
    displayNameSubscription = SubscriptionHelper.unsubscribe(displayNameSubscription);
    displayNameSubscription = interactor.getDisplayName(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::setDisplayName, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          callback.setDisplayName("");
        });
  }

  public void postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem, boolean shouldExclude,
      long ignoreTime, @NonNull PostUnlockCallback callback) {
    postUnlockSubscription = SubscriptionHelper.unsubscribe(postUnlockSubscription);
    postUnlockSubscription =
        interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
            shouldExclude, ignoreTime)
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(result -> {
              Timber.d("onPostUnlock");
              callback.onPostUnlock();
            }, throwable -> {
              Timber.e(throwable, "Error postunlock");
              callback.onLockedError();
            });
  }

  interface LockErrorCallback {

    void onLockedError();
  }

  interface PostUnlockCallback extends LockErrorCallback {

    void onPostUnlock();
  }

  interface DisplayNameLoadCallback {

    void setDisplayName(@NonNull String name);
  }

  interface LockCallback extends LockErrorCallback {

    void onLocked(long lockUntilTime);
  }

  interface IgnoreTimeCallback {
    void onInitializeWithIgnoreTime(long time);
  }

  interface LockHintCallback {

    void setDisplayHint(@NonNull String hint);
  }

  interface LockScreenTypeCallback {
    void onTypeText();

    void onTypePattern();
  }
}
