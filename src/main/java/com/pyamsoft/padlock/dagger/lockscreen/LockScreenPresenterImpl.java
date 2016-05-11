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
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.lockscreen.LockScreen;
import com.pyamsoft.padlock.app.lockscreen.LockScreenPresenter;
import com.pyamsoft.padlock.dagger.lock.LockPresenterImpl;
import javax.inject.Inject;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockScreenPresenterImpl extends LockPresenterImpl<LockScreen>
    implements LockScreenPresenter {

  @NonNull private final LockScreenInteractor lockScreenInteractor;

  @NonNull private Subscription ignoreSubscription = Subscriptions.empty();
  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();
  @NonNull private Subscription displayNameSubscription = Subscriptions.empty();

  @Inject public LockScreenPresenterImpl(final Context context,
      @NonNull final LockScreenInteractor lockScreenInteractor) {
    super(context.getApplicationContext(), lockScreenInteractor);
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
    final LockScreen lockScreen = get();
    if (time == PadLockPreferences.PERIOD_FIVE) {
      lockScreen.setIgnoreTimeFive();
    } else if (time == PadLockPreferences.PERIOD_TEN) {
      lockScreen.setIgnoreTimeTen();
    } else if (time == PadLockPreferences.PERIOD_THIRTY) {
      lockScreen.setIgnoreTimeThirty();
    } else {
      lockScreen.setIgnoreTimeNone();
    }
  }

  @Override public void setIgnorePeriodFromPreferences(@Nullable Long ignoreTime)
      throws NullPointerException {
    unsubIgnoreTime();
    if (ignoreTime == null) {
      ignoreSubscription = lockScreenInteractor.getDefaultIgnoreTime()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(this::setIgnorePeriod, throwable -> {
            Timber.e(throwable, "setIgnorePeriodFromPreferences onError");
            get().setIgnoreTimeError();
          });
    } else {
      setIgnorePeriod(ignoreTime);
    }
  }

  @Override public void lockEntry() {
    final LockScreen lockScreen = get();
    unsubLock();
    lockSubscription =
        lockScreenInteractor.lockEntry(lockScreen.getPackageName(), lockScreen.getActivityName())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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

  @Override public void submit() {
    final LockScreen lockScreen = get();
    unsubUnlock();
    unlockSubscription =
        lockScreenInteractor.unlockEntry(lockScreen.getPackageName(), lockScreen.getActivityName(),
            lockScreen.getCurrentAttempt(), lockScreen.shouldExcludeEntry(),
            lockScreen.getIgnorePeriodTime())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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

  @Override public void loadDisplayNameFromPackage() {
    unsubDisplayName();
    displayNameSubscription = lockScreenInteractor.getDisplayName(get().getPackageName())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(s -> {
          get().setDisplayName(s);
        }, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          get().setDisplayName("");
        });
  }

  private void unsubDisplayName() {
    if (!displayNameSubscription.isUnsubscribed()) {
      displayNameSubscription.unsubscribe();
    }
  }
}
