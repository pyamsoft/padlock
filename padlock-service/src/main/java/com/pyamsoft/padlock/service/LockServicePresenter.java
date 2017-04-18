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

package com.pyamsoft.padlock.service;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.pydroid.bus.EventBus;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockServicePresenter extends SchedulerPresenter {

  @NonNull private final LockServiceInteractor interactor;

  @Inject LockServicePresenter(@NonNull LockServiceInteractor interactor,
      @Named("obs") Scheduler obsScheduler, @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    interactor.reset();
  }

  @Override protected void onStop() {
    super.onStop();
    interactor.cleanup();
  }

  /**
   * public
   */
  void registerOnBus(@NonNull ServiceCallback callback) {
    disposeOnStop(EventBus.get()
        .listen(ServiceFinishEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(serviceEvent -> callback.onFinish(),
            throwable -> Timber.e(throwable, "onError service bus")));

    disposeOnStop(EventBus.get()
        .listen(LockPassEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockPassEvent -> setLockScreenPassed(lockPassEvent.packageName(),
            lockPassEvent.className()), throwable -> Timber.e(throwable, "onError lock pass bus")));

    disposeOnStop(EventBus.get()
        .listen(RecheckEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(recheckEvent -> callback.onRecheck(recheckEvent.packageName(),
            recheckEvent.className()), throwable -> Timber.e(throwable, "onError recheck bus")));
  }

  @SuppressWarnings("WeakerAccess") void setLockScreenPassed(@NonNull String packageName,
      @NonNull String className) {
    interactor.setLockScreenPassed(packageName, className, true);
  }

  /**
   * public
   */
  void processActiveApplicationIfMatching(@NonNull String packageName, @NonNull String className,
      @NonNull ProcessCallback callback) {
    disposeOnStop(interactor.processActiveIfMatching(packageName, className)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(recheck -> {
          if (recheck) {
            processAccessibilityEvent(packageName, className, RecheckStatus.FORCE, callback);
          }
        }, throwable -> Timber.e(throwable, "onError processActiveApplicationIfMatching")));
  }

  /**
   * public
   */
  void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      @NonNull RecheckStatus forcedRecheck, @NonNull ProcessCallback callback) {
    disposeOnStop(interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(padLockEntry -> {
          if (PadLockEntry.isEmpty(padLockEntry)) {
            Timber.w("PadLockEntry is EMPTY, ignore");
          } else {
            callback.startLockScreen(padLockEntry, className);
          }
        }, throwable -> Timber.e(throwable, "Error getting PadLockEntry for LockScreen")));
  }

  interface ProcessCallback {

    void startLockScreen(@NonNull PadLockEntry entry, @NonNull String realName);
  }

  interface ServiceCallback {

    void onFinish();

    void onRecheck(@NonNull String packageName, @NonNull String className);
  }
}
