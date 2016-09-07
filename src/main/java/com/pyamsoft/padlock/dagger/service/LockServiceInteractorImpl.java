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

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.TagConstraint;
import com.pyamsoft.padlock.PadLockSingleInitProvider;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.Singleton;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.lock.LockScreenActivity2;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.job.RecheckJob;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockServiceInteractorImpl implements LockServiceInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final KeyguardManager keyguard;
  @NonNull private final Context appContext;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject LockServiceInteractorImpl(final @NonNull Context context,
      @NonNull PadLockPreferences preferences, @NonNull KeyguardManager keyguard,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.keyguard = keyguard;
  }

  /**
   * Clean up the lock service, cancel background jobs
   */
  @Override public void cleanup() {
    Timber.d("Cleanup LockService");
    Timber.d("Cancel ALL jobs in background");
    Singleton.Jobs.with(appContext)
        .cancelJobsInBackground(null, TagConstraint.ANY, RecheckJob.TAG_ALL);
  }

  /**
   * Return true if the window event is caused by an Activity
   */
  @NonNull @Override public Observable<Boolean> isEventFromActivity(@NonNull String packageName,
      @NonNull String className) {
    Timber.d("Check event from activity: %s %s", packageName, className);
    return packageManagerWrapper.getActivityInfo(packageName, className)
        .map(activityInfo -> activityInfo != null);
  }

  /**
   * Return true if the device is currently locked
   */
  @NonNull @CheckResult @Override public Observable<Boolean> isDeviceLocked() {
    return Observable.defer(() -> {
      boolean locked = false;
      Timber.d("Check if device is currently locked in some secure manner");
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        Timber.d("Device has isDeviceLocked() call");
        locked = keyguard.isDeviceLocked();
      }
      return Observable.just(locked || keyguard.inKeyguardRestrictedInputMode());
    });
  }

  /**
   * If the screen has changed, update the last package.
   * This will prevent the lock screen from opening twice when the same
   * app opens multiple activities for example.
   */
  @NonNull @CheckResult @Override public Observable<Boolean> hasNameChanged(@NonNull String name,
      @NonNull String oldName) {
    return Observable.defer(() -> Observable.just(!name.equals(oldName)));
  }

  @NonNull @Override public Observable<Boolean> isWindowFromLockScreen(@NonNull String packageName,
      @NonNull String className) {
    return Observable.defer(() -> {
      final boolean lockScreen1 =
          packageName.equals(PadLockSingleInitProvider.class.getPackage().getName()) && className.equals(
              LockScreenActivity1.class.getName());
      final boolean lockScreen2 =
          packageName.equals(PadLockSingleInitProvider.class.getPackage().getName()) && className.equals(
              LockScreenActivity2.class.getName());
      return Observable.just(lockScreen1 || lockScreen2);
    });
  }

  @NonNull @Override public Observable<Boolean> isOnlyLockOnPackageChange() {
    return Observable.defer(() -> Observable.just(preferences.getLockOnPackageChange()));
  }

  @NonNull @Override public Observable<Boolean> isLockWhenDeviceLocked() {
    return Observable.defer(() -> Observable.just(preferences.getLockOnDeviceLocked()));
  }

  @NonNull @CheckResult @Override
  public Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Query DB for entry with PN %s and AN %s", packageName, activityName);
    final Observable<PadLockEntry> specificActivityEntry =
        PadLockDB.with(appContext).queryWithPackageActivityName(packageName, activityName).first();
    final Observable<PadLockEntry> packageActivityEntry = PadLockDB.with(appContext)
        .queryWithPackageActivityName(packageName, PadLockEntry.PACKAGE_ACTIVITY_NAME)
        .first();
    return Observable.zip(specificActivityEntry, packageActivityEntry,
        (specificEntry, packageEntry) -> {
          Timber.d("Check the specific entry for validity");
          if (!PadLockEntry.isEmpty(specificEntry)) {
            Timber.d("Specific entry PN %s, AN %s", specificEntry.packageName(),
                specificEntry.activityName());
            return specificEntry;
          }

          Timber.w("Specific entry is the EMPTY entry, try something else");
          if (!PadLockEntry.isEmpty(packageEntry)) {
            Timber.d("Package entry PN %s, AN %s", packageEntry.packageName(),
                packageEntry.activityName());
            return packageEntry;
          }

          Timber.w("Package entry is the EMPTY entry, return NULL");
          return null;
        }).filter(entry -> {
      final boolean filterOut = entry != null;
      Timber.d("Keep entry if not NULL: %s", filterOut);
      return filterOut;
    }).filter(entry -> {
      Timber.d("Check ignore time for: %s %s", entry.packageName(), entry.activityName());
      final long ignoreUntilTime = entry.ignoreUntilTime();
      final long currentTime = System.currentTimeMillis();
      Timber.d("Ignore until time: %d", ignoreUntilTime);
      Timber.d("Current time: %d", currentTime);
      if (currentTime < ignoreUntilTime) {
        Timber.d("Ignore period has not elapsed yet");
        return false;
      }

      Timber.d("Lock: %s %s", entry.packageName(), entry.activityName());
      return true;
    }).filter(entry -> {
      if (entry.activityName().equals(PadLockEntry.PACKAGE_ACTIVITY_NAME) && entry.whitelist()) {
        throw new RuntimeException(
            "PACKAGE entry for package: " + entry.packageName() + " cannot be whitelisted");
      }

      Timber.d("Filter out whitelisted packages");
      return !entry.whitelist();
    });
  }
}
