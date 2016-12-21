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

package com.pyamsoft.padlockpresenter.service;

import android.support.annotation.NonNull;
import com.pyamsoft.padlockmodel.sql.PadLockEntry;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import com.pyamsoft.pydroidrx.SubscriptionHelper;
import javax.inject.Inject;
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
  @SuppressWarnings("WeakerAccess") @NonNull Subscription lockedEntrySubscription =
      Subscriptions.empty();

  @Inject LockServicePresenterImpl(@NonNull final LockServiceStateInteractor stateInteractor,
      @NonNull final LockServiceInteractor interactor, @NonNull Scheduler obsScheduler,
      @NonNull Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
    this.stateInteractor = stateInteractor;
    lockScreenPassed = false;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    SubscriptionHelper.unsubscribe(lockedEntrySubscription);
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

  @SuppressWarnings("WeakerAccess") void reset() {
    Timber.i("Reset name state");
    lastPackageName = "";
    lastClassName = "";
  }

  @Override public void getActiveNames(@NonNull String packageName, @NonNull String className) {
    getView(
        lockService -> lockService.onActiveNamesRetrieved(packageName, activePackageName, className,
            activeClassName));
  }

  @Override
  public void processAccessibilityEvent(@NonNull String packageName, @NonNull String className,
      @NonNull Recheck forcedRecheck) {
    SubscriptionHelper.unsubscribe(lockedEntrySubscription);
    final Observable<Boolean> windowEventObservable =
        stateInteractor.isServiceEnabled()
            .filter(enabled -> {
              if (!enabled) {
                Timber.e("Service is not user-enabled. Ignore");
                reset();
              }
              return enabled;
            })
            .flatMap(enabled -> interactor.isDeviceLocked().map(deviceLocked -> {
              if (deviceLocked) {
                Timber.w("Device is locked, reset lastPackage/lastClass");
                reset();
              }
              return enabled;
            }))
            .flatMap(enabled -> interactor.isEventFromActivity(packageName, className))
            .filter(fromActivity -> {
              if (!fromActivity) {
                Timber.w("Event is not caused by an Activity. P: %s, C: %s. Ignore", packageName,
                    className);
              }
              return fromActivity;
            })
            .flatMap(fromActivity -> interactor.isDeviceLocked().flatMap(deviceLocked -> {
              if (deviceLocked) {
                return interactor.isRestrictedWhileLocked();
              } else {
                return Observable.just(false);
              }
            }))
            .filter(restrictedWhileLocked -> {
              if (restrictedWhileLocked) {
                Timber.w("Locking is restricted while device in keyguard: %s %s", packageName,
                    className);
                return false;
              } else {
                return true;
              }
            })
            .flatMap(notLocked -> interactor.isWindowFromLockScreen(packageName, className))
            .filter(isLockScreen -> {
              if (isLockScreen) {
                Timber.w("Event for package %s class: %s is caused by LockScreen. Ignore",
                    packageName, className);
              }
              return !isLockScreen;
            })
            .map(fromLockScreen -> {
              final boolean passed = !fromLockScreen;
              Timber.i("Window has passed checks so far: %s", passed);
              activePackageName = packageName;
              activeClassName = className;
              return passed;
            });

    lockedEntrySubscription = Observable.zip(windowEventObservable,
        interactor.hasNameChanged(packageName, lastPackageName),
        interactor.hasNameChanged(className, lastClassName), interactor.isOnlyLockOnPackageChange(),
        (windowEventObserved, packageChanged, classChanged, lockOnPackageChange) -> {
          if (!windowEventObserved) {
            Timber.e("Failed to pass window checking");
            return false;
          }

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

          if (forcedRecheck.equals(Recheck.FORCE)) {
            Timber.d("Pass filter via forced recheck");
            windowHasChanged = true;
          }

          return windowHasChanged || !lockScreenPassed;
        })
        .filter(lockApp -> lockApp)
        .flatMap(aBoolean -> {
          Timber.d("Get list of locked classes with package: %s, class: %s", packageName,
              className);
          setLockScreenPassed(false);
          return interactor.getEntry(packageName, className);
        })
        .doOnNext(entry -> {
          if (PadLockEntry.isEmpty(entry)) {
            Timber.w("Returned entry is EMPTY");
          } else {
            Timber.d("Default entry PN %s, AN %s", entry.packageName(), entry.activityName());
          }
        })
        .filter(padLockEntry -> !PadLockEntry.isEmpty(padLockEntry))
        .filter(entry -> {
          Timber.d("Check ignore time for: %s %s", entry.packageName(), entry.activityName());
          final long ignoreUntilTime = entry.ignoreUntilTime();
          final long currentTime = System.currentTimeMillis();
          Timber.d("Ignore until time: %d", ignoreUntilTime);
          Timber.d("Current time: %d", currentTime);
          if (currentTime < ignoreUntilTime) {
            Timber.d("Ignore period has not elapsed yet");
            return false;
          }

          return true;
        })
        .filter(entry -> {
          if (PadLockEntry.PACKAGE_ACTIVITY_NAME.equals(entry.activityName())
              && entry.whitelist()) {
            throw new RuntimeException(
                "PACKAGE entry for package: " + entry.packageName() + " cannot be whitelisted");
          }

          Timber.d("Filter out if whitelisted packages");
          return !entry.whitelist();
        })
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(padLockEntry -> {
              Timber.d("Got PadLockEntry for LockScreen: %s %s", padLockEntry.packageName(),
                  padLockEntry.activityName());
              launchCorrectLockScreen(padLockEntry, className);
            }, throwable -> Timber.e(throwable, "Error getting PadLockEntry for LockScreen"),
            () -> SubscriptionHelper.unsubscribe(lockedEntrySubscription));
  }

  @SuppressWarnings("WeakerAccess") void launchCorrectLockScreen(@NonNull PadLockEntry entry,
      @NonNull String realName) {
    getView(lockService -> lockService.startLockScreen1(entry, realName));
  }
}
