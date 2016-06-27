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
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func4;
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

  public final void processAccessibilityEvent(@NonNull String packageName,
      @NonNull String className) {
    unsubLockedEntry();
    final Observable<Boolean> windowEventObservable =
        stateInteractor.isServiceEnabled().filter(new Func1<Boolean, Boolean>() {
          @Override public Boolean call(Boolean enabled) {
            if (!enabled) {
              Timber.e("Service is not user-enabled");
              reset();
            }
            return enabled;
          }
        }).zipWith(interactor.isLockWhenDeviceLocked(), new Func2<Boolean, Boolean, Boolean>() {
          @Override public Boolean call(Boolean enabled, Boolean lockWhenDeviceLocked) {
            Timber.d("Check if device should reset on lock");
            return enabled && lockWhenDeviceLocked;
          }
        }).zipWith(interactor.isDeviceLocked(), new Func2<Boolean, Boolean, Boolean>() {
          @Override public Boolean call(Boolean isLockWhenDeviceLocked, Boolean isDeviceLocked) {
            if (isLockWhenDeviceLocked && isDeviceLocked) {
              Timber.d("Device is Locked. Reset state");
              reset();
            }

            // Always return true here to continue
            return true;
          }
        }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
          @Override public Observable<Boolean> call(Boolean aBoolean) {
            Timber.d("Check if event is from activity");
            return interactor.isEventFromActivity(packageName, className);
          }
        }).filter(new Func1<Boolean, Boolean>() {
          @Override public Boolean call(Boolean fromActivity) {
            if (!fromActivity) {
              Timber.e("Event is not caused by an Activity. P: %s, C: %s", packageName, className);
            }
            return fromActivity;
          }
        }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
          @Override public Observable<Boolean> call(Boolean aBoolean) {
            Timber.d("Check if window is from lock screen");
            return interactor.isWindowFromLockScreen(packageName, className);
          }
        }).filter(new Func1<Boolean, Boolean>() {
          @Override public Boolean call(Boolean isLockScreen) {
            if (isLockScreen) {
              Timber.e("Event for package %s class: %s is caused by LockScreen", packageName,
                  className);
            }
            return !isLockScreen;
          }
        });

    lockedEntrySubscription = Observable.zip(windowEventObservable,
        interactor.hasNameChanged(packageName, lastPackageName),
        interactor.hasNameChanged(className, lastClassName), interactor.isOnlyLockOnPackageChange(),
        new Func4<Boolean, Boolean, Boolean, Boolean, Boolean>() {
          @Override public Boolean call(Boolean windowEventObserved, Boolean packageChanged,
              Boolean classChanged, Boolean lockOnPackageChange) {
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

            return windowHasChanged;
          }
        })
        .filter(new Func1<Boolean, Boolean>() {
          @Override public Boolean call(Boolean windowHasChanged) {
            return windowHasChanged || !lockScreenPassed;
          }
        })
        .flatMap(new Func1<Boolean, Observable<PadLockEntry>>() {
          @Override public Observable<PadLockEntry> call(Boolean aBoolean) {
            Timber.d("Get list of locked classes with package: %s, class: %s", packageName,
                className);
            setLockScreenPassed(false);
            return interactor.getEntry(packageName, className);
          }
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

  public interface LockService {

    void startLockScreen(@NonNull PadLockEntry entry);
  }
}
