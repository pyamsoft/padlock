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

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.TagConstraint;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.PadLockSingleInitProvider;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.lock.LockScreenActivity2;
import com.pyamsoft.padlock.dagger.sql.PadLockDB;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.job.RecheckJob;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockServiceInteractorImpl implements LockServiceInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final JobManager jobManager;
  @NonNull private final Context appContext;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject LockServiceInteractorImpl(final @NonNull Context context,
      @NonNull PadLockPreferences preferences, @NonNull JobManager jobManager,
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    this.jobManager = jobManager;
    this.packageManagerWrapper = packageManagerWrapper;
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
  }

  /**
   * Clean up the lock service, cancel background jobs
   */
  @Override public void cleanup() {
    Timber.d("Cleanup LockService");
    Timber.d("Cancel ALL jobs in background");
    jobManager.cancelJobsInBackground(null, TagConstraint.ANY, RecheckJob.TAG_ALL);
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
          packageName.equals(PadLockSingleInitProvider.class.getPackage().getName())
              && className.equals(LockScreenActivity1.class.getName());
      final boolean lockScreen2 =
          packageName.equals(PadLockSingleInitProvider.class.getPackage().getName())
              && className.equals(LockScreenActivity2.class.getName());
      return Observable.just(lockScreen1 || lockScreen2);
    });
  }

  @NonNull @Override public Observable<Boolean> isOnlyLockOnPackageChange() {
    return Observable.defer(() -> Observable.just(preferences.getLockOnPackageChange()));
  }

  @NonNull @CheckResult @Override
  public Observable<PadLockEntry> getEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Query DB for entry with PN %s and AN %s", packageName, activityName);
    return PadLockDB.with(appContext)
        .queryWithPackageActivityNameDefault(packageName, activityName)
        .first()
        .map(entry -> {
          if (PadLockEntry.isEmpty(entry)) {
            Timber.w("Returned entry is EMPTY");
            return null;
          }

          Timber.d("Default entry PN %s, AN %s", entry.packageName(), entry.activityName());
          return entry;
        })
        .filter(entry -> entry != null)
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
          if (entry.activityName().equals(PadLockEntry.PACKAGE_ACTIVITY_NAME)
              && entry.whitelist()) {
            throw new RuntimeException(
                "PACKAGE entry for package: " + entry.packageName() + " cannot be whitelisted");
          }

          Timber.d("Filter out if whitelisted packages");
          return !entry.whitelist();
        });
  }
}
