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

package com.pyamsoft.padlock.dagger.lock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.lock.LockScreen;
import com.pyamsoft.padlock.app.lock.LockScreenPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockScreenPresenterImpl extends LockPresenterImpl<LockScreen> implements LockScreenPresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final LockScreenInteractor interactor;
  @SuppressWarnings("WeakerAccess") @NonNull final AppIconLoaderPresenter<LockScreen> iconLoader;
  @NonNull private Subscription displayNameSubscription = Subscriptions.empty();
  @NonNull private Subscription hintSubscription = Subscriptions.empty();
  @NonNull private Subscription ignoreTimeSubscription = Subscriptions.empty();
  @NonNull private Subscription postUnlockSubscription = Subscriptions.empty();
  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();

  @Inject LockScreenPresenterImpl(@NonNull AppIconLoaderPresenter<LockScreen> iconLoader,
      @NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.iconLoader = iconLoader;
    this.interactor = lockScreenInteractor;
    interactor.resetFailCount();
  }

  @Override protected void onBind() {
    super.onBind();
    getView(iconLoader::bindView);
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    iconLoader.unbindView();
    unsubIgnoreTime();
    unsubUnlock();
    unsubLock();
    unsubDisplayName();
    unsubPostUnlock();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    iconLoader.destroy();
    interactor.resetFailCount();
  }

  @SuppressWarnings("WeakerAccess") void unsubPostUnlock() {
    if (!postUnlockSubscription.isUnsubscribed()) {
      postUnlockSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubUnlock() {
    if (!unlockSubscription.isUnsubscribed()) {
      unlockSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubLock() {
    if (!lockSubscription.isUnsubscribed()) {
      lockSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubHint() {
    if (!hintSubscription.isUnsubscribed()) {
      hintSubscription.unsubscribe();
    }
  }

  @Override public void displayLockedHint() {
    unsubHint();
    hintSubscription = interactor.getHint()
        .map(s -> s == null ? "" : s)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(hint -> getView(lockScreen -> lockScreen.setDisplayHint(hint)), throwable -> {
          Timber.e(throwable, "onError displayLockedHint");
          // TODO
        }, this::unsubHint);
  }

  @Override public void createWithDefaultIgnoreTime() {
    unsubIgnoreTime();
    ignoreTimeSubscription = interactor.getDefaultIgnoreTime()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(time -> getView(lockScreen -> lockScreen.initializeWithIgnoreTime(time)),
            throwable -> {
              Timber.e(throwable, "onError createWithDefaultIgnoreTime");
              // TODO
            }, this::unsubIgnoreTime);
  }

  @SuppressWarnings("WeakerAccess") void unsubIgnoreTime() {
    if (!ignoreTimeSubscription.isUnsubscribed()) {
      ignoreTimeSubscription.unsubscribe();
    }
  }

  @Override public void lockEntry(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, long ignoreUntilTime, boolean isSystem) {
    unsubLock();
    lockSubscription = interactor.incrementAndGetFailCount()
        .filter(count -> count > LockScreenInteractor.DEFAULT_MAX_FAIL_COUNT)
        .flatMap(integer -> interactor.getTimeoutPeriodMinutesInMillis())
        .flatMap(timeOutMinutesInMillis -> {
          final long newLockUntilTime = System.currentTimeMillis() + timeOutMinutesInMillis;
          Timber.d("Lock %s %s until %d (%d)", packageName, activityName, newLockUntilTime,
              timeOutMinutesInMillis);
          return interactor.lockEntry(packageName, activityName, lockCode, newLockUntilTime,
              ignoreUntilTime, isSystem);
        })
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockTime -> {
          Timber.d("Received lock entry result");
          getView(lockScreen -> lockScreen.onLocked(lockTime));
        }, throwable -> {
          Timber.e(throwable, "lockEntry onError");
          getView(LockScreen::onLockedError);
          unsubLock();
        }, this::unsubLock);
  }

  @Override public void submit(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, @NonNull String currentAttempt) {
    unsubUnlock();
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
        .flatMap(new Func1<String, Observable<Boolean>>() {
          @Override public Observable<Boolean> call(String nullablePin) {
            return interactor.unlockEntry(currentAttempt, nullablePin);
          }
        })
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(unlocked -> getView(lockScreen -> {
          Timber.d("Received unlock entry result");
          if (unlocked) {
            lockScreen.onSubmitSuccess();
          } else {
            lockScreen.onSubmitFailure();
          }
        }), throwable -> {
          Timber.e(throwable, "unlockEntry onError");
          getView(LockScreen::onSubmitError);
          unsubUnlock();
        }, this::unsubUnlock);
  }

  @Override public void loadDisplayNameFromPackage(@NonNull String packageName) {
    unsubDisplayName();
    displayNameSubscription = interactor.getDisplayName(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(s -> getView(lockScreen -> lockScreen.setDisplayName(s)), throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          getView(lockScreen -> lockScreen.setDisplayName(""));
        }, this::unsubDisplayName);
  }

  @Override public void postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, long lockUntilTime, boolean isSystem,
      boolean selectedExclude, long selectedIgnoreTime) {

    final long ignoreMinutesInMillis = selectedIgnoreTime * 60 * 1000;
    final Observable<Long> whitelistObservable;
    final Observable<Integer> ignoreObservable;
    final Observable<Integer> recheckObservable;

    if (selectedExclude) {
      whitelistObservable =
          interactor.whitelistEntry(packageName, activityName, realName, lockCode, isSystem);
    } else {
      whitelistObservable = Observable.just(0L);
    }

    if (selectedIgnoreTime != 0 && !selectedExclude) {
      ignoreObservable =
          interactor.ignoreEntryForTime(packageName, activityName, lockCode, lockUntilTime,
              ignoreMinutesInMillis, isSystem);
    } else {
      ignoreObservable = Observable.just(0);
    }

    if (selectedIgnoreTime != 0 && !selectedExclude) {
      recheckObservable =
          interactor.queueRecheckJob(packageName, activityName, ignoreMinutesInMillis);
    } else {
      recheckObservable = Observable.just(0);
    }

    unsubPostUnlock();
    postUnlockSubscription =
        Observable.zip(ignoreObservable, recheckObservable, whitelistObservable,
            (ignore, recheck, whitelist) -> {
              Timber.d("Result of Whitelist: %d", whitelist);
              Timber.d("Result of Ignore: %d", ignore);
              Timber.d("Result of Recheck: %d", recheck);

              // KLUDGE Just return something valid for now
              return true;
            })
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(result -> {
              Timber.d("onPostUnlock");
              getView(LockScreen::onPostUnlock);
            }, throwable -> {
              Timber.e(throwable, "Error postunlock");
              getView(LockScreen::onLockedError);
            }, this::unsubPostUnlock);
  }

  @SuppressWarnings("WeakerAccess") void unsubDisplayName() {
    if (!displayNameSubscription.isUnsubscribed()) {
      displayNameSubscription.unsubscribe();
    }
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    iconLoader.loadApplicationIcon(packageName);
  }
}
