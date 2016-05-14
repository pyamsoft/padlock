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
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockServicePresenterImpl extends PresenterImpl<LockServicePresenter.LockService>
    implements LockServicePresenter {

  @NonNull private final LockServiceInteractor interactor;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription lockedEntrySubscription = Subscriptions.empty();

  private boolean lockScreenPassed;
  @NonNull private String lastPackageName = "";
  @NonNull private String lastClassName = "";

  @Inject public LockServicePresenterImpl(@NonNull final LockServiceStateInteractor stateInteractor,
      @NonNull final LockServiceInteractor interactor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.interactor = interactor;
    this.stateInteractor = stateInteractor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  private void setLockScreenPassed(boolean b) {
    Timber.d("Set lockScreenPassed: %s", b);
    lockScreenPassed = b;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubLockedEntry();
  }

  @Override public void setLockScreenPassed() {
    setLockScreenPassed(true);
  }

  @Override public void setLockScreenNotPassed() {
    setLockScreenPassed(false);
  }

  @NonNull private Observable<PadLockEntry> getLockScreen() {
    Timber.d("Get list of locked classes with package: %s", lastPackageName);
    return interactor.getEntry(lastPackageName, lastClassName)
        .mapToOne(PadLockEntry.MAPPER::map)
        .filter(padLockEntry -> padLockEntry != null)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler);
  }

  private void unsubLockedEntry() {
    if (!lockedEntrySubscription.isUnsubscribed()) {
      lockedEntrySubscription.unsubscribe();
    }
  }

  @Override
  public void processAccessibilityEvent(@NonNull String packageName, @NonNull String className) {
    unsubLockedEntry();
    lockedEntrySubscription = Observable.defer(() -> {
      if (!stateInteractor.isServiceEnabled()) {
        Timber.e("Service is not user-enabled");
        return Observable.empty();
      }

      Timber.d("Last Package: %s - New Package: %s", lastPackageName, packageName);
      Timber.d("Last Class: %s - New Class: %s", lastClassName, className);

      if (interactor.isEventCausedByNotificationShade(packageName, className)) {
        Timber.i("Notification shade. Event will be ignored");
        return Observable.empty();
      }

      if (interactor.isComingFromLockScreen(lastClassName) && lockScreenPassed) {
        Timber.i("Coming from LockActivity, do not show again.");
        return Observable.empty();
      }

      if (interactor.isDeviceLocked()) {
        Timber.i("Device is Locked. Reset state");
        lastPackageName = "";
        lastClassName = "";
      }

      if (interactor.isNameHardUnlocked(packageName, className)) {
        Timber.i("Class or package is hardcoded to never be locked");
        Timber.i("P: %s C: %s", packageName, className);
        return Observable.empty();
      }

      final boolean packageChanged = interactor.hasNameChanged(packageName, lastPackageName,
          LockServiceInteractor.GOOGLE_KEYBOARD_PACKAGE_REGEX);
      if (packageChanged) {
        Timber.i("Last package changed from: %s to: %s", lastPackageName, packageName);
        lastPackageName = packageName;
      }

      final boolean classChanged = interactor.hasNameChanged(className, lastClassName,
          LockServiceInteractor.ANDROID_VIEW_CLASS_REGEX);
      if (classChanged) {
        Timber.i("Last class changed from: %s to: %s", lastClassName, className);
        lastClassName = className;
      }

      if (packageChanged && classChanged || !lockScreenPassed) {
        return getLockScreen();
      } else {
        return Observable.empty();
      }
    }).subscribeOn(ioScheduler).observeOn(mainScheduler).subscribe(padLockEntry -> {
      Timber.d("Got PadLockEntry for LockScreen: ", padLockEntry.packageName());
      final LockService lockService = getView();
      if (lockService != null) {
        lockService.startLockScreen(padLockEntry);
      }

      Timber.d("Unsub from observable");
      unsubLockedEntry();
    }, throwable -> {
      Timber.e(throwable, "Error getting PadLockEntry for LockScreen");
    });
  }
}
