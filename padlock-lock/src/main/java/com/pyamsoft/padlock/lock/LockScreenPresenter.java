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
import com.pyamsoft.padlock.lock.common.LockTypePresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockScreenPresenter extends LockTypePresenter {

  @NonNull private final LockScreenInteractor interactor;

  @Inject LockScreenPresenter(@NonNull LockScreenInteractor lockScreenInteractor,
      @NonNull @Named("obs") Scheduler obsScheduler, @NonNull @Named("io") Scheduler subScheduler) {
    super(lockScreenInteractor, obsScheduler, subScheduler);
    this.interactor = lockScreenInteractor;
  }

  /**
   * public
   */
  void createWithDefaultIgnoreTime(@NonNull IgnoreTimeCallback callback) {
    disposeOnStop(interactor.getDefaultIgnoreTime()
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .subscribe(callback::onInitializeWithIgnoreTime,
            throwable -> Timber.e(throwable, "onError createWithDefaultIgnoreTime")));
  }

  /**
   * public
   */
  void loadDisplayNameFromPackage(@NonNull String packageName,
      @NonNull DisplayNameLoadCallback callback) {
    disposeOnStop(interactor.getDisplayName(packageName)
        .subscribeOn(getBackgroundScheduler())
        .observeOn(getForegroundScheduler())
        .subscribe(callback::setDisplayName, throwable -> {
          Timber.e(throwable, "Error loading display name from package");
          callback.setDisplayName("");
        }));
  }

  /**
   * public
   */
  void closeOldAndAwaitSignal(@NonNull String packageName, @NonNull String activityName,
      @NonNull CloseCallback callback) {
    // Send bus event first before we register or we may catch our own event.
    //EventBus.get().publish(CloseOldEvent.create(packageName, activityName));
    //
    //disposeOnStop(EventBus.get()
    //    .listen(CloseOldEvent.class)
    //    .filter(closeOldEvent -> closeOldEvent.packageName().equals(packageName)
    //        && closeOldEvent.activityName().equals(activityName))
    //    .subscribeOn(getBackgroundScheduler())
    //    .observeOn(getForegroundScheduler())
    //    .subscribe(closeOldEvent -> {
    //      Timber.w("Received a CloseOld event: %s %s", closeOldEvent.packageName(),
    //          closeOldEvent.activityName());
    //      callback.onCloseOldReceived();
    //    }, throwable -> Timber.e(throwable, "error bus close old")));
  }

  interface CloseCallback {

    void onCloseOldReceived();
  }

  interface LockErrorCallback {

    void onLockedError();
  }

  interface DisplayNameLoadCallback {

    void setDisplayName(@NonNull String name);
  }

  interface IgnoreTimeCallback {
    void onInitializeWithIgnoreTime(long time);
  }
}
