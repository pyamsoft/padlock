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

import android.support.annotation.CheckResult;
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

  @NonNull private final LockScreenInteractor lockScreenInteractor;
  private final long ignoreTimeNone;
  private final long ignoreTimeFive;
  private final long ignoreTimeTen;
  private final long ignoreTimeThirty;

  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();
  @NonNull private Subscription displayNameSubscription = Subscriptions.empty();

  @Inject public LockScreenPresenter(@NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("main") Scheduler mainScheduler, @NonNull @Named("io") Scheduler ioScheduler,
      @Named("ignore_none") long ignoreTimeNone, @Named("ignore_five") long ignoreTimeFive,
      @Named("ignore_ten") long ignoreTimeTen, @Named("ignore_thirty") long ignoreTimeThirty) {
    super(mainScheduler, ioScheduler);
    this.ignoreTimeNone = ignoreTimeNone;
    this.ignoreTimeFive = ignoreTimeFive;
    this.ignoreTimeTen = ignoreTimeTen;
    this.ignoreTimeThirty = ignoreTimeThirty;
    this.lockScreenInteractor = lockScreenInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubUnlock();
    unsubLock();
    unsubDisplayName();
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
    if (time == ignoreTimeFive) {
      lockScreen.setIgnoreTimeFive();
    } else if (time == ignoreTimeTen) {
      lockScreen.setIgnoreTimeTen();
    } else if (time == ignoreTimeThirty) {
      lockScreen.setIgnoreTimeThirty();
    } else {
      lockScreen.setIgnoreTimeNone();
    }
  }

  public final void setIgnorePeriodFromPreferences(@Nullable Long ignoreTime)
      throws NullPointerException {
    if (ignoreTime == null) {
      final long defaultIgnoreTime = lockScreenInteractor.getDefaultIgnoreTime();
      setIgnorePeriod(defaultIgnoreTime);
    } else {
      setIgnorePeriod(ignoreTime);
    }
  }

  public final void lockEntry(@NonNull String packageName, @NonNull String activityName) {
    unsubLock();
    final LockScreen lockScreen = getView();
    lockSubscription = lockScreenInteractor.lockEntry(packageName, activityName)
        .subscribeOn(getIoScheduler())
        .observeOn(getMainScheduler())
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

  public final void submit(@NonNull String packageName, @NonNull String activityName,
      @NonNull String currentAttempt, boolean excludeEntry, long ignorePeriodTime) {
    unsubUnlock();
    final LockScreen lockScreen = getView();
    unlockSubscription =
        lockScreenInteractor.unlockEntry(packageName, activityName, currentAttempt, excludeEntry,
            ignorePeriodTime)
            .subscribeOn(getIoScheduler())
            .observeOn(getMainScheduler())
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
    displayNameSubscription = lockScreenInteractor.getDisplayName(packageName)
        .subscribeOn(getIoScheduler())
        .observeOn(getMainScheduler())
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

  @CheckResult public long getIgnoreTimeNone() {
    return ignoreTimeNone;
  }

  @CheckResult public long getIgnoreTimeFive() {
    return ignoreTimeFive;
  }

  @CheckResult public long getIgnoreTimeTen() {
    return ignoreTimeTen;
  }

  @CheckResult public long getIgnoreTimeThirty() {
    return ignoreTimeThirty;
  }
}
