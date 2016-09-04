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

import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.service.LockServicePresenter;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.dagger.presenter.SchedulerPresenter;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockServicePresenterImpl extends SchedulerPresenter<LockServicePresenter.LockService>
    implements LockServicePresenter {

  @SuppressWarnings("WeakerAccess") @NonNull final LockServiceInteractor interactor;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull String lastPackageName = "";
  @SuppressWarnings("WeakerAccess") @NonNull String lastClassName = "";
  @SuppressWarnings("WeakerAccess") @NonNull String activePackageName = "";
  @SuppressWarnings("WeakerAccess") @NonNull String activeClassName = "";
  @SuppressWarnings("WeakerAccess") boolean lockScreenPassed;
  @NonNull private Subscription lockedEntrySubscription = Subscriptions.empty();
  @NonNull private Subscription pickCorrectSubscription = Subscriptions.empty();

  @Inject LockServicePresenterImpl(@NonNull final LockServiceStateInteractor stateInteractor,
      @NonNull final LockServiceInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.interactor = interactor;
    this.stateInteractor = stateInteractor;
    lockScreenPassed = false;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubLockedEntry();
    unsubPickCorrect();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    interactor.cleanup();
  }

  @SuppressWarnings("WeakerAccess") void setLockScreenPassed(boolean b) {
    Timber.d("Set lockScreenPassed: %s", b);
    lockScreenPassed = b;
  }

  @Override public void setLockScreenPassed() {
    setLockScreenPassed(true);
  }

  @SuppressWarnings("WeakerAccess") void unsubLockedEntry() {
    if (!lockedEntrySubscription.isUnsubscribed()) {
      lockedEntrySubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubPickCorrect() {
    if (!pickCorrectSubscription.isUnsubscribed()) {
      pickCorrectSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void reset() {
    Timber.i("Reset name state");
    lastPackageName = "";
    lastClassName = "";
  }

  @Override @NonNull @CheckResult public String getActiveClassName() {
    return activeClassName;
  }

  @Override @NonNull @CheckResult public String getActivePackageName() {
    return activePackageName;
  }

  @Override
  public void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      boolean forcedRecheck) {
    unsubLockedEntry();
    final Observable<Boolean> windowEventObservable =
        stateInteractor.isServiceEnabled().filter(enabled -> {
          if (!enabled) {
            Timber.e("Service is not user-enabled");
            reset();
          }
          return enabled;
        }).zipWith(interactor.isLockWhenDeviceLocked(), (enabled, lockWhenDeviceLocked) -> {
          Timber.d("Check if device should reset on lock");
          return enabled && lockWhenDeviceLocked;
        }).zipWith(interactor.isDeviceLocked(), (isLockWhenDeviceLocked, isDeviceLocked) -> {
          if (isLockWhenDeviceLocked && isDeviceLocked) {
            Timber.d("Device is Locked. Reset state");
            reset();
          }

          // Always return true here to continue
          return true;
        }).flatMap(aBoolean -> {
          Timber.d("Check if event is from activity");
          return interactor.isEventFromActivity(packageName, className);
        }).filter(fromActivity -> {
          if (!fromActivity) {
            Timber.e("Event is not caused by an Activity. P: %s, C: %s", packageName, className);
          }
          return fromActivity;
        }).flatMap(aBoolean -> {
          Timber.d("Check if window is from lock screen");
          return interactor.isWindowFromLockScreen(packageName, className);
        }).filter(isLockScreen -> {
          if (isLockScreen) {
            Timber.e("Event for package %s class: %s is caused by LockScreen", packageName,
                className);
          }
          return !isLockScreen;
        }).map(valid -> {
          Timber.d("Window has passed checks so far: %s", valid);
          activePackageName = packageName;
          activeClassName = className;
          return valid;
        });

    lockedEntrySubscription = Observable.zip(windowEventObservable,
        interactor.hasNameChanged(packageName, lastPackageName),
        interactor.hasNameChanged(className, lastClassName), interactor.isOnlyLockOnPackageChange(),
        (windowEventObserved, packageChanged, classChanged, lockOnPackageChange) -> {
          if (packageChanged) {
            Timber.d("Last Package: %s - New Package: %s", lastPackageName, packageName);
            lastPackageName = packageName;
          }
          if (classChanged) {
            Timber.d("Last Class: %s - New Class: %s", lastClassName, className);
            lastClassName = className;
          }

          Timber.d("Window change if class changed");
          boolean windowHasChanged = classChanged;
          if (lockOnPackageChange) {
            Timber.d("Window change if package changed");
            windowHasChanged &= packageChanged;
          }

          if (forcedRecheck) {
            Timber.d("Pass filter via forced recheck");
            windowHasChanged = true;
          }

          return windowHasChanged;
        })
        .filter(windowHasChanged -> windowHasChanged || !lockScreenPassed)
        .flatMap(aBoolean -> {
          Timber.d("Get list of locked classes with package: %s, class: %s", packageName,
              className);
          setLockScreenPassed(false);
          return interactor.getEntry(packageName, className);
        })
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(padLockEntry -> {
              Timber.d("Got PadLockEntry for LockScreen: %s %s", padLockEntry.packageName(),
                  padLockEntry.activityName());
              launchCorrectLockScreen(padLockEntry, className);
            }, throwable -> Timber.e(throwable, "Error getting PadLockEntry for LockScreen"),
            this::unsubLockedEntry);
  }

  @SuppressWarnings("WeakerAccess") void launchCorrectLockScreen(@NonNull PadLockEntry entry,
      @NonNull String realName) {
    unsubPickCorrect();
    pickCorrectSubscription = interactor.isExperimentalNSupported()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .map(nSupported -> nSupported
            && LockScreenActivity1.isActive()
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        .subscribe(launchActivity2 -> {
          if (launchActivity2) {
            getView().startLockScreen2(entry, realName);
          } else {
            getView().startLockScreen1(entry, realName);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          // TODO error
        }, this::unsubPickCorrect);
  }
}
