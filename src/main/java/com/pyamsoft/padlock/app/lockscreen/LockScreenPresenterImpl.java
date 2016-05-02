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

package com.pyamsoft.padlock.app.lockscreen;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.lock.LockInteractor;
import com.pyamsoft.padlock.app.lock.LockPresenterImpl;
import com.pyamsoft.padlock.app.lock.LockView;
import com.pyamsoft.padlock.model.event.LockButtonClickEvent;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockScreenPresenterImpl extends LockPresenterImpl<LockScreen>
    implements LockScreenPresenter {

  @NonNull private final LockScreenInteractor lockScreenInteractor;

  @NonNull private Subscription ignoreSubscription = Subscriptions.empty();
  @NonNull private Subscription unlockSubscription = Subscriptions.empty();
  @NonNull private Subscription lockSubscription = Subscriptions.empty();

  @Inject public LockScreenPresenterImpl(final Context context,
      @NonNull final LockInteractor lockInteractor,
      @NonNull final LockScreenInteractor lockScreenInteractor) {
    super(context.getApplicationContext(), lockInteractor);
    this.lockScreenInteractor = lockScreenInteractor;
  }

  @Override public void unbind() {
    super.unbind();
    unsubIgnoreTime();
    unsubUnlock();
    unsubLock();
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
      ignoreSubscription =
          Observable.defer(() -> Observable.just(lockScreenInteractor.getDefaultIgnoreTime()))
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

  @Override protected LockButtonClickEvent takeCommand() throws NullPointerException {
    final LockView lockView = get();
    final String attempt = lockView.getCurrentAttempt();
    return LockButtonClickEvent.builder()
        .code(attempt)
        .status(LockButtonClickEvent.STATUS_COMMAND)
        .build();
  }

  @Override protected LockButtonClickEvent clickButton(char button) throws NullPointerException {
    final LockScreen lockScreen = get();
    final String attempt = lockScreen.getCurrentAttempt();
    final String code = buttonClicked(button, attempt);
    final boolean submittable = lockScreenInteractor.isSubmittable(code);
    return LockButtonClickEvent.builder()
        .status(submittable ? LockButtonClickEvent.STATUS_SUBMITTABLE
            : LockButtonClickEvent.STATUS_NONE)
        .code(code)
        .build();
  }

  @Override public void lockEntry() {
    final LockScreen lockScreen = get();
    unsubLock();
    lockSubscription =
        lockScreenInteractor.lockEntry(lockScreen.getPackageName(), lockScreen.getActivityName())
            .subscribe(unlocked -> {
              Timber.d("Received lock entry result");
              if (unlocked) {
                lockScreen.onLocked();
              } else {
                lockScreen.onLockedError();
              }

              unsubLock();
            }, throwable -> {
              Timber.e(throwable, "lockEntry onError");
              lockScreen.onLockedError();
            });
  }

  @Override public void unlockEntry() {
    final LockScreen lockScreen = get();
    final String code = lockScreen.getCurrentAttempt();
    if (lockScreenInteractor.isSubmittable(code)) {
      unsubUnlock();
      unlockSubscription = lockScreenInteractor.unlockEntry(lockScreen.getPackageName(),
          lockScreen.getActivityName(), lockScreen.getCurrentAttempt(),
          lockScreen.shouldExcludeEntry(), lockScreen.getIgnorePeriodTime()).subscribe(unlocked -> {
        Timber.d("Received unlock entry result");
        if (unlocked) {
          lockScreen.onUnlockSuccess();
        } else {
          lockScreen.onUnlockFailure();
        }

        unsubUnlock();
      }, throwable -> {
        Timber.e(throwable, "unlockEntry onError");
        lockScreen.onUnlockError();
      });
    } else {
      lockScreen.onSubmitError();
    }
  }
}
