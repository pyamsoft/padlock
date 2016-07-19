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

import android.os.Build;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.SchedulerPresenter;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
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
  @NonNull private Subscription pickCorrectSubscription = Subscriptions.empty();

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

  @Override protected void onUnbind(@NonNull LockService view) {
    super.onUnbind(view);
    unsubLockedEntry();
    unsubPickCorrect();
    interactor.cleanup();
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

  private void unsubPickCorrect() {
    if (!pickCorrectSubscription.isUnsubscribed()) {
      pickCorrectSubscription.unsubscribe();
    }
  }

  private void reset() {
    Timber.i("Reset name state");
    lastPackageName = "";
    lastClassName = "";
  }

  public final void processAccessibilityEvent(@NonNull String packageName,
      @NonNull String className, boolean forcedRecheck) {
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
        }).doOnNext(valid -> {
          Timber.d("Window has passed checks so far: %s", valid);
          getView().updateCurrentWindowValues(packageName, className);
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
          final LockService lockService = getView();
          lockService.startLockScreen(padLockEntry);
        }, throwable -> {
          Timber.e(throwable, "Error getting PadLockEntry for LockScreen");
        });
  }

  public void launchCorrectLockScreen(@NonNull String packageName, @NonNull String activityName) {
    unsubPickCorrect();
    interactor.isExperimentalNSupported()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .map(nSupported -> nSupported
            && LockScreenActivity1.isActive()
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        .subscribe(launchActivity2 -> {
          if (launchActivity2) {
            getView().startLockScreen2(packageName, activityName);
          } else {
            getView().startLockScreen1(packageName, activityName);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          // TODO error
        });
  }

  public interface LockService {

    void updateCurrentWindowValues(@NonNull String packageName, @NonNull String className);

    void startLockScreen(@NonNull PadLockEntry entry);

    void startLockScreen1(@NonNull String packageName, @NonNull String activityName);

    void startLockScreen2(@NonNull String packageName, @NonNull String activityName);
  }
}
