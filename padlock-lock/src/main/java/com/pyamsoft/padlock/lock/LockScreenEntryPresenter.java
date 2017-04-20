/*
 * Copyright 2017 Peter Kenji Yamanaka
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
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockScreenEntryPresenter extends SchedulerPresenter {

  @NonNull private final LockScreenEntryInteractor interactor;

  @Inject LockScreenEntryPresenter(@NonNull LockScreenEntryInteractor lockScreenInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = lockScreenInteractor;
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    interactor.resetFailCount();
  }

  /**
   * public
   */
  void displayLockedHint(@NonNull LockHintCallback callback) {
    disposeOnStop(interactor.getHint()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(callback::setDisplayHint,
            throwable -> Timber.e(throwable, "onError displayLockedHint")));
  }

  /**
   * public
   */
  void lockEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull LockCallback callback) {
    disposeOnStop(interactor.lockEntryOnFail(packageName, activityName)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(timePair -> {
          if (timePair.currentTime < timePair.lockUntilTime) {
            Timber.d("Received lock entry result");
            callback.onLocked(timePair.lockUntilTime);
          } else {
            Timber.w("No timeout period set, entry not locked");
          }
        }, throwable -> {
          Timber.e(throwable, "lockEntry onError");
          callback.onLockedError();
        }));
  }

  /**
   * public
   */
  void submit(@NonNull String packageName, @NonNull String activityName, @Nullable String lockCode,
      long lockUntilTime, @NonNull String currentAttempt, @NonNull LockSubmitCallback callback) {
    disposeOnStop(
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
            }));
  }

  /**
   * public
   */
  void postUnlock(@NonNull String packageName, @NonNull String activityName,
      @NonNull String realName, @Nullable String lockCode, boolean isSystem, boolean shouldExclude,
      long ignoreTime, @NonNull PostUnlockCallback callback) {
    disposeOnStop(interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
        shouldExclude, ignoreTime)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(() -> {
          Timber.d("onPostUnlock");
          callback.onPostUnlock();
        }, throwable -> {
          Timber.e(throwable, "Error postunlock");
          callback.onLockedError();
        }));
  }

  interface LockErrorCallback {

    void onLockedError();
  }

  interface PostUnlockCallback extends LockErrorCallback {

    void onPostUnlock();
  }

  interface LockCallback extends LockErrorCallback {

    void onLocked(long lockUntilTime);
  }

  interface LockHintCallback {

    void setDisplayHint(@NonNull String hint);
  }
}
