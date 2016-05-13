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

package com.pyamsoft.padlock.dagger.lockscreen;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.lockscreen.LockScreen;
import com.pyamsoft.padlock.app.lockscreen.LockScreenPresenter;
import com.pyamsoft.padlock.dagger.lock.LockPresenterImpl;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockScreenPresenterImpl extends LockPresenterImpl<LockScreen>
    implements LockScreenPresenter {

  @NonNull private final LockScreenInteractor lockScreenInteractor;
  private final long ignoreTimeNone;
  private final long ignoreTimeFive;
  private final long ignoreTimeTen;
  private final long ignoreTimeThirty;

  @NonNull private Subscription ignoreSubscription = Subscriptions.empty();
  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();
  @NonNull private Subscription displayNameSubscription = Subscriptions.empty();

  @Inject public LockScreenPresenterImpl(final Context context,
      @NonNull final LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("main") Scheduler mainScheduler, @NonNull @Named("io") Scheduler ioScheduler,
      @Named("ignore_none") long ignoreTimeNone, @Named("ignore_five") long ignoreTimeFive,
      @Named("ignore_ten") long ignoreTimeTen, @Named("ignore_thirty") long ignoreTimeThirty) {
    super(context.getApplicationContext(), lockScreenInteractor, mainScheduler, ioScheduler);
    this.ignoreTimeNone = ignoreTimeNone;
    this.ignoreTimeFive = ignoreTimeFive;
    this.ignoreTimeTen = ignoreTimeTen;
    this.ignoreTimeThirty = ignoreTimeThirty;
    this.lockScreenInteractor = lockScreenInteractor;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubIgnoreTime();
    unsubUnlock();
    unsubLock();
    unsubDisplayName();
  }

  private void unsubIgnoreTime() {
    if (!ignoreSubscription.isUnsubscribed()) {
      ignoreSubscription.unsubscribe();
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

  private void setIgnorePeriod(final long time) throws NullPointerException {
    final LockScreen lockScreen = getView();
    if (lockScreen != null) {
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
  }

  @Override public void setIgnorePeriodFromPreferences(@Nullable Long ignoreTime)
      throws NullPointerException {
    unsubIgnoreTime();
    if (ignoreTime == null) {
      ignoreSubscription = lockScreenInteractor.getDefaultIgnoreTime()
          .subscribeOn(getIoScheduler())
          .observeOn(getMainScheduler())
          .subscribe(this::setIgnorePeriod, throwable -> {
            Timber.e(throwable, "setIgnorePeriodFromPreferences onError");
            final LockScreen lockScreen = getView();
            if (lockScreen != null) {
              lockScreen.setIgnoreTimeError();
            }
          });
    } else {
      setIgnorePeriod(ignoreTime);
    }
  }

  @Override public void lockEntry() {
    unsubLock();
    final LockScreen lockScreen = getView();
    if (lockScreen != null) {
      lockSubscription =
          lockScreenInteractor.lockEntry(lockScreen.getPackageName(), lockScreen.getActivityName())
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
  }

  @Override public void submit() {
    unsubUnlock();
    final LockScreen lockScreen = getView();
    if (lockScreen != null) {
      unlockSubscription = lockScreenInteractor.unlockEntry(lockScreen.getPackageName(),
          lockScreen.getActivityName(), lockScreen.getCurrentAttempt(),
          lockScreen.shouldExcludeEntry(), lockScreen.getIgnorePeriodTime())
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
  }

  @Override public void loadDisplayNameFromPackage() {
    unsubDisplayName();
    final LockScreen lockScreen = getView();
    if (lockScreen != null) {
      displayNameSubscription = lockScreenInteractor.getDisplayName(lockScreen.getPackageName())
          .subscribeOn(getIoScheduler())
          .observeOn(getMainScheduler())
          .subscribe(lockScreen::setDisplayName, throwable -> {
            Timber.e(throwable, "Error loading display name from package");
            lockScreen.setDisplayName("");
          });
    }
  }

  private void unsubDisplayName() {
    if (!displayNameSubscription.isUnsubscribed()) {
      displayNameSubscription.unsubscribe();
    }
  }

  @Override public long getIgnoreTimeNone() {
    return ignoreTimeNone;
  }

  @Override public long getIgnoreTimeFive() {
    return ignoreTimeFive;
  }

  @Override public long getIgnoreTimeTen() {
    return ignoreTimeTen;
  }

  @Override public long getIgnoreTimeThirty() {
    return ignoreTimeThirty;
  }
}
