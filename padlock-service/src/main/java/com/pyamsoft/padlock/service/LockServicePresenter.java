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
import com.pyamsoft.pydroid.helper.DisposableHelper;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

class LockServicePresenter extends SchedulerPresenter {

  @NonNull private final LockServiceInteractor interactor;
  @NonNull private Disposable lockedEntryDisposable = Disposables.empty();
  @NonNull private Disposable recheckDisposable = Disposables.empty();
  @NonNull private Disposable serviceBus = Disposables.empty();
  @NonNull private Disposable recheckBus = Disposables.empty();
  @NonNull private Disposable passBus = Disposables.empty();

  @Inject LockServicePresenter(@NonNull LockServiceInteractor interactor,
      @Named("obs") Scheduler obsScheduler, @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    interactor.reset();
  }

  @Override protected void onStop() {
    super.onStop();
    lockedEntryDisposable = DisposableHelper.dispose(lockedEntryDisposable);
    recheckDisposable = DisposableHelper.dispose(recheckDisposable);
    serviceBus = DisposableHelper.dispose(serviceBus);
    recheckBus = DisposableHelper.dispose(recheckBus);
    passBus = DisposableHelper.dispose(passBus);
    interactor.cleanup();
  }

  /**
   * public
   */
  void registerOnBus(@NonNull ServiceCallback callback) {
    serviceBus = DisposableHelper.dispose(serviceBus);
    serviceBus = EventBus.get()
        .listen(ServiceFinishEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(serviceEvent -> {
          callback.onFinish();
        }, throwable -> Timber.e(throwable, "onError service bus"));

    passBus = DisposableHelper.dispose(passBus);
    passBus = EventBus.get()
        .listen(LockPassEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockPassEvent -> setLockScreenPassed(lockPassEvent.packageName(),
            lockPassEvent.className()), throwable -> Timber.e(throwable, "onError lock pass bus"));

    recheckBus = DisposableHelper.dispose(recheckBus);
    recheckBus = EventBus.get()
        .listen(RecheckEvent.class)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(recheckEvent -> callback.onRecheck(recheckEvent.packageName(),
            recheckEvent.className()), throwable -> Timber.e(throwable, "onError recheck bus"));
  }

  void setLockScreenPassed(@NonNull String packageName, @NonNull String className) {
    interactor.setLockScreenPassed(packageName, className, true);
  }

  /**
   * public
   */
  void processActiveApplicationIfMatching(@NonNull String packageName, @NonNull String className,
      @NonNull ProcessCallback callback) {
    recheckDisposable = DisposableHelper.dispose(recheckDisposable);
    recheckDisposable = interactor.processActiveIfMatching(packageName, className)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(recheck -> {
          if (recheck) {
            processAccessibilityEvent(packageName, className, RecheckStatus.FORCE, callback);
          }
        }, throwable -> Timber.e(throwable, "onError processActiveApplicationIfMatching"));
  }

  /**
   * public
   */
  void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      @NonNull RecheckStatus forcedRecheck, @NonNull ProcessCallback callback) {
    lockedEntryDisposable = DisposableHelper.dispose(lockedEntryDisposable);
    lockedEntryDisposable = interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(padLockEntry -> {
          if (PadLockEntry.isEmpty(padLockEntry)) {
            Timber.w("PadLockEntry is EMPTY, ignore");
          } else {
            callback.startLockScreen(padLockEntry, className);
          }
        }, throwable -> Timber.e(throwable, "Error getting PadLockEntry for LockScreen"));
  }

  interface ProcessCallback {

    void startLockScreen(@NonNull PadLockEntry entry, @NonNull String realName);
  }

  interface ServiceCallback {

    void onFinish();

    void onRecheck(@NonNull String packageName, @NonNull String className);
  }
}
