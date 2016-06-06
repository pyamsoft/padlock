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

package com.pyamsoft.padlock.dagger.service;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.service.LockServicePresenter;
import com.pyamsoft.padlock.dagger.base.SchedulerPresenterImpl;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockServicePresenterImpl
    extends SchedulerPresenterImpl<LockServicePresenter.LockService>
    implements LockServicePresenter {

  @NonNull private final LockServiceInteractor interactor;
  @NonNull private final LockServiceStateInteractor stateInteractor;

  @NonNull private Subscription lockedEntrySubscription = Subscriptions.empty();

  private boolean running;
  private boolean lockScreenPassed;
  @NonNull private String lastPackageName = "";
  @NonNull private String lastClassName = "";

  @Inject public LockServicePresenterImpl(@NonNull final LockServiceStateInteractor stateInteractor,
      @NonNull final LockServiceInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
    this.stateInteractor = stateInteractor;
    running = false;
    lockScreenPassed = false;
  }

  @Override public void onCreateView(@NonNull LockService view) {
    super.onCreateView(view);
    running = true;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    running = false;
    unsubLockedEntry();
  }

  @Override public boolean isRunning() {
    return running;
  }

  private void setLockScreenPassed(boolean b) {
    Timber.d("Set lockScreenPassed: %s", b);
    lockScreenPassed = b;
  }

  @Override public void setLockScreenPassed() {
    setLockScreenPassed(true);
  }

  private void unsubLockedEntry() {
    if (!lockedEntrySubscription.isUnsubscribed()) {
      lockedEntrySubscription.unsubscribe();
    }
  }

  private void reset() {
    Timber.i("Reset name state");
    lastPackageName = "";
    lastClassName = "";
  }

  @Override
  public void processAccessibilityEvent(@NonNull String packageName, @NonNull String className) {
    unsubLockedEntry();
    lockedEntrySubscription = Observable.defer(() -> {
      if (!stateInteractor.isServiceEnabled()) {
        Timber.e("Service is not user-enabled");
        reset();
        return Observable.empty();
      }

      if (interactor.isLockWhenDeviceLocked()) {
        if (interactor.isDeviceLocked()) {
          Timber.i("Device is Locked. Reset state");
          reset();
        }
      }

      if (!interactor.isEventFromActivity(packageName, className)) {
        Timber.e("Event is not caused by an Activity. P: %s, C: %s", packageName, className);
        return Observable.empty();
      }

      if (interactor.isWindowFromLockScreen(packageName, className)) {
        Timber.e("Event for package %s class: %s is caused by LockScreen", packageName, className);
        return Observable.empty();
      }

      final boolean packageChanged = interactor.hasNameChanged(packageName, lastPackageName);
      if (packageChanged) {
        Timber.d("Last Package: %s - New Package: %s", lastPackageName, packageName);
        lastPackageName = packageName;
      }

      final boolean classChanged = interactor.hasNameChanged(className, lastClassName);
      if (classChanged) {
        Timber.d("Last Class: %s - New Class: %s", lastClassName, className);
        lastClassName = className;
      }

      // By default, the window will respond to a change event if the class changes
      Timber.d("Window change if class changed");
      boolean windowHasChanged = classChanged;
      if (interactor.isOnlyLockOnPackageChange()) {
        Timber.d("Window change if package changed");
        windowHasChanged &= packageChanged;
      }

      if (windowHasChanged || !lockScreenPassed) {
        Timber.d("Get list of locked classes with package: %s, class: %s", packageName, className);
        setLockScreenPassed(false);
        return interactor.getEntry(packageName, className);
      } else {
        Timber.d("No significant window change detected");
        return Observable.empty();
      }
    }).subscribeOn(getIoScheduler()).observeOn(getMainScheduler()).subscribe(padLockEntry -> {
      Timber.d("Got PadLockEntry for LockScreen: %s %s", padLockEntry.packageName(),
          padLockEntry.activityName());
      final LockService lockService = getView();
      if (lockService != null) {
        lockService.startLockScreen(padLockEntry);
      }
    }, throwable -> {
      Timber.e(throwable, "Error getting PadLockEntry for LockScreen");
    });
  }
}
