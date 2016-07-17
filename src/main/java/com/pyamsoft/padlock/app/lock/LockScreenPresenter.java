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

package com.pyamsoft.padlock.app.lock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import com.pyamsoft.padlock.dagger.lock.LockScreenInteractor;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockScreenPresenter extends LockPresenter<LockScreen> {

  @NonNull private final LockScreenInteractor interactor;

  @NonNull private Subscription postUnlockSubscription = Subscriptions.empty();
  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();
  @NonNull private Subscription displayNameSubscription = Subscriptions.empty();

  @Inject public LockScreenPresenter(@NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = lockScreenInteractor;
    interactor.resetFailCount();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubUnlock();
    unsubLock();
    unsubDisplayName();
    unsubPostUnlock();
    interactor.resetFailCount();
  }

  private void unsubPostUnlock() {
    if (!postUnlockSubscription.isUnsubscribed()) {
      postUnlockSubscription.unsubscribe();
    }
  }

  private void unsubUnlock() {
    if (!unlockSubscription.isUnsubscribed()) {
      unlockSubscription.unsubscribe();
    }
  }

  private void unsubLock() {
    if (!lockSubscription.isUnsubscribed()) {
      lockSubscription.unsubscribe();
    }
  }

  private void setIgnorePeriod(final long time) {
    final LockScreen lockScreen = getView();
    if (time == interactor.getIgnoreTimeOne().toBlocking().first()) {
      lockScreen.setIgnoreTimeOne();
    } else if (time == interactor.getIgnoreTimeFive().toBlocking().first()) {
      lockScreen.setIgnoreTimeFive();
    } else if (time == interactor.getIgnoreTimeTen().toBlocking().first()) {
      lockScreen.setIgnoreTimeTen();
    } else if (time == interactor.getIgnoreTimeFifteen().toBlocking().first()) {
      lockScreen.setIgnoreTimeFifteen();
    } else if (time == interactor.getIgnoreTimeTwenty().toBlocking().first()) {
      lockScreen.setIgnoreTimeTwenty();
    } else if (time == interactor.getIgnoreTimeThirty().toBlocking().first()) {
      lockScreen.setIgnoreTimeThirty();
    } else if (time == interactor.getIgnoreTimeFourtyFive().toBlocking().first()) {
      lockScreen.setIgnoreTimeFourtyFive();
    } else if (time == interactor.getIgnoreTimeSixty().toBlocking().first()) {
      lockScreen.setIgnoreTimeSixty();
    } else {
      lockScreen.setIgnoreTimeNone();
    }
  }

  public final void saveSelectedOptions(int selectedIndex) {
    final long time = interactor.getIgnoreTimeForIndex(selectedIndex).toBlocking().first();
    getView().onSaveMenuSelections(time);
  }

  public final void setIgnorePeriodFromPreferences(@Nullable Long ignoreTime) {
    if (ignoreTime == null) {
      final long defaultIgnoreTime = interactor.getDefaultIgnoreTime().toBlocking().first();
      setIgnorePeriod(defaultIgnoreTime);
    } else {
      setIgnorePeriod(ignoreTime);
    }
  }

  public final void lockEntry(@NonNull String packageName, @NonNull String activityName) {
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

  public final void submit(@NonNull String packageName, @NonNull String activityName,
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

  public final void loadDisplayNameFromPackage(@NonNull String packageName) {
    unsubDisplayName();
    final LockScreen lockScreen = getView();
    displayNameSubscription = interactor.getDisplayName(packageName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockScreen::setDisplayName, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          lockScreen.setDisplayName("");
        });
  }

  public void postUnlock(@NonNull String packageName, @NonNull String activityName,
      boolean shouldExclude, int ignoreIndex, int recheckIndex) {
    unsubPostUnlock();
    final Observable<Long> ignore = interactor.getIgnoreTimeForIndex(ignoreIndex);
    // TODO resolve recheck time from unique interactor function
    final Observable<Long> recheck = interactor.getIgnoreTimeForIndex(recheckIndex);
    postUnlockSubscription = Observable.zip(ignore, recheck, Pair::new)
        .flatMap(pair -> interactor.postUnlock(packageName, activityName, shouldExclude, pair.first,
            pair.second))
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(result -> {
          Timber.d("onPostUnlock");
          getView().onPostUnlock();
        }, throwable -> {
          Timber.e(throwable, "Error postunlock");
          getView().onLockedError();
        });
  }

  private void unsubDisplayName() {
    if (!displayNameSubscription.isUnsubscribed()) {
      displayNameSubscription.unsubscribe();
    }
  }
}
