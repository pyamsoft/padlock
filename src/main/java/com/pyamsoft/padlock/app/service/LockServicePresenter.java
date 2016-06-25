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

package com.pyamsoft.padlock.app.service;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.SchedulerPresenter;
import com.pyamsoft.padlock.dagger.service.LockServiceInteractor;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockServicePresenter
    extends SchedulerPresenter<LockServicePresenter.LockService> {

  @NonNull private final LockServiceInteractor interactor;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @NonNull private Subscription lockedEntrySubscription = Subscriptions.empty();

  @NonNull private String lastPackageName = "";
  @NonNull private String lastClassName = "";
  private boolean lockScreenPassed;

  @Inject public LockServicePresenter(@NonNull final LockServiceStateInteractor stateInteractor,
      @NonNull final LockServiceInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
    this.stateInteractor = stateInteractor;
    lockScreenPassed = false;
  }

  @Override protected void onBind() {
    super.onBind();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubLockedEntry();
  }

  private void setLockScreenPassed(boolean b) {
    Timber.d("Set lockScreenPassed: %s", b);
    lockScreenPassed = b;
  }

  public final void setLockScreenPassed() {
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

  public final void processAccessibilityEvent(@NonNull String packageName, @NonNull String className) {
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
    }).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler()).subscribe(padLockEntry -> {
      Timber.d("Got PadLockEntry for LockScreen: %s %s", padLockEntry.packageName(),
          padLockEntry.activityName());
      final LockService lockService = getView();
      lockService.startLockScreen(padLockEntry);
    }, throwable -> {
      Timber.e(throwable, "Error getting PadLockEntry for LockScreen");
    });
  }

  public interface LockService {

    void startLockScreen(@NonNull PadLockEntry entry);
  }
}
