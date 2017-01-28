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
import com.pyamsoft.pydroid.presenter.Presenter;
import com.pyamsoft.pydroid.rx.SchedulerPresenter;
import com.pyamsoft.pydroid.rx.SubscriptionHelper;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockScreenPresenterImpl extends SchedulerPresenter<Presenter.Empty>
    implements LockScreenPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final LockScreenInteractor interactor;
  @SuppressWarnings("WeakerAccess") @NonNull Subscription postUnlockSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription displayNameSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription ignoreTimeSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription unlockSubscription =
      Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription lockSubscription = Subscriptions.empty();
  @SuppressWarnings("WeakerAccess") @NonNull Subscription hintSubscription = Subscriptions.empty();

  @Inject LockScreenPresenterImpl(@NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = lockScreenInteractor;
    interactor.resetFailCount();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(ignoreTimeSubscription, unlockSubscription, lockSubscription,
        displayNameSubscription, postUnlockSubscription);
  }

  @Override public void resetFailCount() {
    interactor.resetFailCount();
  }

  @Override public void displayLockedHint(@NonNull LockHintCallback callback) {
    SubscriptionHelper.unsubscribe(hintSubscription);
    hintSubscription = interactor.getHint()
        .map(s -> s == null ? "" : s)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::setDisplayHint, throwable -> {
          Timber.e(throwable, "onError displayLockedHint");
          // TODO
        }, () -> SubscriptionHelper.unsubscribe(hintSubscription));
  }

  @Override public void createWithDefaultIgnoreTime(@NonNull IgnoreTimeCallback callback) {
    SubscriptionHelper.unsubscribe(ignoreTimeSubscription);
    ignoreTimeSubscription = interactor.getDefaultIgnoreTime()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::initializeWithIgnoreTime, throwable -> {
          Timber.e(throwable, "onError createWithDefaultIgnoreTime");
          // TODO
        }, () -> SubscriptionHelper.unsubscribe(ignoreTimeSubscription));
  }

  @Override public void lockEntry(@NonNull String packageName, @NonNull String activityName,
      long lockUntilTime, @NonNull LockCallback callback) {
    SubscriptionHelper.unsubscribe(lockSubscription);
    lockSubscription = interactor.incrementAndGetFailCount()
        .filter(count -> count > LockScreenInteractor.DEFAULT_MAX_FAIL_COUNT)
        .flatMap(integer -> interactor.getTimeoutPeriodMinutesInMillis())
        .flatMap(timeOutMinutesInMillis -> {
          final long newLockUntilTime = System.currentTimeMillis() + timeOutMinutesInMillis;
          Timber.d("Lock %s %s until %d (%d)", packageName, activityName, newLockUntilTime,
              timeOutMinutesInMillis);
          return interactor.lockEntry(newLockUntilTime, packageName, activityName);
        })
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockTime -> {
          Timber.d("Received lock entry result");
          callback.onLocked(lockTime);
        }, throwable -> {
          Timber.e(throwable, "lockEntry onError");
          callback.onLockedError();
        }, () -> SubscriptionHelper.unsubscribe(lockSubscription));
  }

  @Override public void submit(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, @NonNull String currentAttempt,
      @NonNull LockSubmitCallback callback) {
    SubscriptionHelper.unsubscribe(unlockSubscription);
    unlockSubscription = interactor.getMasterPin()
        .map(masterPin -> {
          Timber.d("Attempt unlock: %s %s", packageName, activityName);
          Timber.d("Check entry is not locked: %d", lockUntilTime);
          if (System.currentTimeMillis() < lockUntilTime) {
            Timber.e("Entry is still locked. Fail unlock");
            return null;
          }

          final String pin;
          if (lockCode == null) {
            Timber.d("No app specific code, use Master PIN");
            pin = masterPin;
          } else {
            Timber.d("App specific code present, compare attempt");
            pin = lockCode;
          }
          return pin;
        })
        .flatMap(nullablePin -> interactor.unlockEntry(currentAttempt, nullablePin))
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
        }, () -> SubscriptionHelper.unsubscribe(unlockSubscription));
  }

  @Override public void loadDisplayNameFromPackage(@NonNull String packageName,
      @NonNull DisplayNameLoadCallback callback) {
    SubscriptionHelper.unsubscribe(displayNameSubscription);
    displayNameSubscription = interactor.getDisplayName(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::setDisplayName, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          callback.setDisplayName("");
        }, () -> SubscriptionHelper.unsubscribe(displayNameSubscription));
  }

  @Override public void postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, long lockUntilTime, boolean isSystem,
      boolean shouldExclude, long ignoreTime, @NonNull PostUnlockCallback callback) {
    final long ignoreMinutesInMillis = ignoreTime * 60 * 1000;
    final Observable<Long> whitelistObservable;
    final Observable<Integer> ignoreObservable;
    final Observable<Integer> recheckObservable;

    if (shouldExclude) {
      whitelistObservable =
          interactor.whitelistEntry(packageName, activityName, realName, lockCode, isSystem);
    } else {
      whitelistObservable = Observable.just(0L);
    }

    if (ignoreTime != 0 && !shouldExclude) {
      ignoreObservable =
          interactor.ignoreEntryForTime(ignoreMinutesInMillis, packageName, activityName);
      recheckObservable =
          interactor.queueRecheckJob(packageName, activityName, ignoreMinutesInMillis);
    } else {
      ignoreObservable = Observable.just(0);
      recheckObservable = Observable.just(0);
    }

    SubscriptionHelper.unsubscribe(postUnlockSubscription);
    postUnlockSubscription =
        Observable.zip(ignoreObservable, recheckObservable, whitelistObservable,
            (ignore, recheck, whitelist) -> {
              Timber.d("Result of Whitelist: %d", whitelist);
              Timber.d("Result of Ignore: %d", ignore);
              Timber.d("Result of Recheck: %d", recheck);

              // KLUDGE Just return something valid for now
              return Boolean.TRUE;
            })
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(result -> {
              Timber.d("onPostUnlock");
              callback.onPostUnlock();
            }, throwable -> {
              Timber.e(throwable, "Error postunlock");
              callback.onLockedError();
            }, () -> SubscriptionHelper.unsubscribe(postUnlockSubscription));
  }
}
