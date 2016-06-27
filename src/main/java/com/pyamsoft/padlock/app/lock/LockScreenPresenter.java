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
import com.pyamsoft.padlock.dagger.lock.LockScreenInteractor;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockScreenPresenter extends LockPresenter<LockScreen> {

  @NonNull private final LockScreenInteractor interactor;

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
    interactor.resetFailCount();
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
    if (time == interactor.getIgnoreTimeOne()) {
      lockScreen.setIgnoreTimeOne();
    } else if (time == interactor.getIgnoreTimeFive()) {
      lockScreen.setIgnoreTimeFive();
    } else if (time == interactor.getIgnoreTimeTen()) {
      lockScreen.setIgnoreTimeTen();
    } else if (time == interactor.getIgnoreTimeFifteen()) {
      lockScreen.setIgnoreTimeFifteen();
    } else if (time == interactor.getIgnoreTimeTwenty()) {
      lockScreen.setIgnoreTimeTwenty();
    } else if (time == interactor.getIgnoreTimeThirty()) {
      lockScreen.setIgnoreTimeThirty();
    } else if (time == interactor.getIgnoreTimeFourtyFive()) {
      lockScreen.setIgnoreTimeFourtyFive();
    } else if (time == interactor.getIgnoreTimeSixty()) {
      lockScreen.setIgnoreTimeSixty();
    } else {
      lockScreen.setIgnoreTimeNone();
    }
  }

  public final void saveSelectedOptions(int selectedIndex) {
    final long time = interactor.getIgnoreTimeForIndex(selectedIndex);
    getView().onSaveMenuSelections(time);
  }

  public final void setIgnorePeriodFromPreferences(@Nullable Long ignoreTime) {
    if (ignoreTime == null) {
      final long defaultIgnoreTime = interactor.getDefaultIgnoreTime();
      setIgnorePeriod(defaultIgnoreTime);
    } else {
      setIgnorePeriod(ignoreTime);
    }
  }

  public final void lockEntry(@NonNull String packageName, @NonNull String activityName) {
    unsubLock();
    if (interactor.incrementAndGetFailCount() > LockScreenInteractor.DEFAULT_MAX_FAIL_COUNT) {
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
      @NonNull String currentAttempt, boolean excludeEntry, int ignoreOptionIndex) {
    unsubUnlock();
    final LockScreen lockScreen = getView();
    unlockSubscription =
        interactor.unlockEntry(packageName, activityName, currentAttempt, excludeEntry,
            interactor.getIgnoreTimeForIndex(ignoreOptionIndex))
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

  private void unsubDisplayName() {
    if (!displayNameSubscription.isUnsubscribed()) {
      displayNameSubscription.unsubscribe();
    }
  }
}
