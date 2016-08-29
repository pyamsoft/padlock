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
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockScreenPresenterImpl extends LockPresenterImpl<LockScreen> implements LockScreenPresenter {

  @NonNull final LockScreenInteractor interactor;
  @NonNull final AppIconLoaderPresenter<LockScreen> iconLoader;

  @NonNull Subscription postUnlockSubscription = Subscriptions.empty();
  @NonNull Subscription unlockSubscription = Subscriptions.empty();
  @NonNull Subscription lockSubscription = Subscriptions.empty();
  @NonNull Subscription displayNameSubscription = Subscriptions.empty();
  @NonNull Subscription hintSubscription = Subscriptions.empty();
  @NonNull Subscription ignoreTimeSubscription = Subscriptions.empty();

  @Inject LockScreenPresenterImpl(@NonNull AppIconLoaderPresenter<LockScreen> iconLoader,
      @NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.iconLoader = iconLoader;
    this.interactor = lockScreenInteractor;
    interactor.resetFailCount();
  }

  @Override protected void onBind(@NonNull LockScreen view) {
    super.onBind(view);
    iconLoader.bindView(view);
  }

  @Override protected void onUnbind(@NonNull LockScreen view) {
    super.onUnbind(view);
    iconLoader.unbindView();
    unsubIgnoreTime();
    unsubUnlock();
    unsubLock();
    unsubDisplayName();
    unsubPostUnlock();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    iconLoader.destroyView();
    interactor.resetFailCount();
  }

  void unsubPostUnlock() {
    if (!postUnlockSubscription.isUnsubscribed()) {
      postUnlockSubscription.unsubscribe();
    }
  }

  void unsubUnlock() {
    if (!unlockSubscription.isUnsubscribed()) {
      unlockSubscription.unsubscribe();
    }
  }

  void unsubLock() {
    if (!lockSubscription.isUnsubscribed()) {
      lockSubscription.unsubscribe();
    }
  }

  void unsubHint() {
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
        .subscribe(hint -> getView().setDisplayHint(hint), throwable -> {
          Timber.e(throwable, "onError displayLockedHint");
          // TODO
        }, this::unsubHint);
  }

  @Override public void createWithDefaultIgnoreTime() {
    unsubIgnoreTime();
    ignoreTimeSubscription = interactor.getDefaultIgnoreTime()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(time -> getView().initializeWithIgnoreTime(time), throwable -> {
          Timber.e(throwable, "onError createWithDefaultIgnoreTime");
          // TODO
        }, this::unsubIgnoreTime);
  }

  void unsubIgnoreTime() {
    if (!ignoreTimeSubscription.isUnsubscribed()) {
      ignoreTimeSubscription.unsubscribe();
    }
  }

  @Override public void lockEntry(@NonNull String packageName, @NonNull String activityName) {
    unsubLock();
    if (interactor.incrementAndGetFailCount().toBlocking().first()
        > LockScreenInteractor.DEFAULT_MAX_FAIL_COUNT) {
      final LockScreen lockScreen = getView();
      lockSubscription = interactor.lockEntry(packageName, activityName)
          .subscribeOn(getSubscribeScheduler())
          .observeOn(getObserveScheduler())
          .subscribe(unlocked -> {
            Timber.d("Received lock entry result");
            if (unlocked) {
              lockScreen.onLocked();
            } else {
              lockScreen.onLockedError();
            }
          }, throwable -> {
            Timber.e(throwable, "lockEntry onError");
            lockScreen.onLockedError();
            unsubLock();
          }, this::unsubLock);
    }
  }

  @Override public void submit(@NonNull String packageName, @NonNull String activityName,
      @NonNull String currentAttempt) {
    unsubUnlock();
    final LockScreen lockScreen = getView();
    unlockSubscription = interactor.unlockEntry(packageName, activityName, currentAttempt)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(unlocked -> {
          Timber.d("Received unlock entry result");
          if (unlocked) {
            lockScreen.onSubmitSuccess();
          } else {
            lockScreen.onSubmitFailure();
          }
        }, throwable -> {
          Timber.e(throwable, "unlockEntry onError");
          lockScreen.onSubmitError();
          unsubUnlock();
        }, this::unsubUnlock);
  }

  @Override public void loadDisplayNameFromPackage(@NonNull String packageName) {
    unsubDisplayName();
    final LockScreen lockScreen = getView();
    displayNameSubscription = interactor.getDisplayName(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockScreen::setDisplayName, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          lockScreen.setDisplayName("");
        }, this::unsubDisplayName);
  }

  @Override public void postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem, boolean shouldExclude,
      long ignoreTime) {
    unsubPostUnlock();
    postUnlockSubscription = Observable.defer(() -> Observable.just(ignoreTime))
        .flatMap(
            time -> interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
                shouldExclude, time))
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(result -> {
          Timber.d("onPostUnlock");
          getView().onPostUnlock();
        }, throwable -> {
          Timber.e(throwable, "Error postunlock");
          getView().onLockedError();
        }, this::unsubPostUnlock);
  }

  void unsubDisplayName() {
    if (!displayNameSubscription.isUnsubscribed()) {
      displayNameSubscription.unsubscribe();
    }
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    iconLoader.loadApplicationIcon(packageName);
  }
}
