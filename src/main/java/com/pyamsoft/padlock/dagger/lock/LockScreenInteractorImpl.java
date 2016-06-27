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

package com.pyamsoft.padlock.dagger.lock;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.sql.PadLockOpenHelper;
import com.pyamsoft.padlock.dagger.db.DBInteractor;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

final class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @NonNull private final MasterPinInteractor pinInteractor;
  @NonNull private final DBInteractor dbInteractor;
  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferences preferences;
  @NonNull private final long[] ignoreTimes;
  private int failCount;

  @Inject public LockScreenInteractorImpl(final @NonNull Context context,
      @NonNull final PadLockPreferences preferences, @NonNull final DBInteractor dbInteractor,
      @NonNull final MasterPinInteractor masterPinInteractor) {
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.dbInteractor = dbInteractor;
    this.pinInteractor = masterPinInteractor;
    this.ignoreTimes = preferences.getIgnoreTimes();
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Boolean> lockEntry(@NonNull String packageName, @NonNull String activityName) {
    Timber.d("Lock entry: %s %s", packageName, activityName);
    return PadLockOpenHelper.queryWithPackageActivityName(appContext, packageName, activityName)
        .first()
        .map(padLockEntry -> {
          Timber.d("LOCKED entry, update entry in DB: ", padLockEntry);
          final long timeOutMinutesInMillis = preferences.getTimeoutPeriod() * 60 * 1000;
          final ContentValues contentValues = PadLockEntry.FACTORY.marshal()
              .packageName(padLockEntry.packageName())
              .activityName(padLockEntry.activityName())
              .lockCode(padLockEntry.lockCode())
              .lockUntilTime(System.currentTimeMillis() + timeOutMinutesInMillis)
              .ignoreUntilTime(padLockEntry.ignoreUntilTime())
              .systemApplication(padLockEntry.systemApplication())
              .asContentValues();
          PadLockOpenHelper.updateWithPackageActivityName(appContext, contentValues,
              padLockEntry.packageName(), padLockEntry.activityName());
          return true;
        });
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<Boolean> unlockEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull String attempt, boolean shouldExclude, long ignoreForPeriod) {
    Timber.d("Attempt unlock: %s %s", packageName, activityName);
    final Observable<PadLockEntry> dbObservable =
        PadLockOpenHelper.queryWithPackageActivityName(appContext, packageName, activityName)
            .first();

    final Observable<String> masterPinObservable =
        Observable.defer(() -> Observable.just(pinInteractor.getMasterPin()));

    return Observable.zip(dbObservable, masterPinObservable, (padLockEntry, masterPin) -> {
      final long lockUntilTime = padLockEntry.lockUntilTime();
      Timber.d("Check entry is not locked");
      if (System.currentTimeMillis() < lockUntilTime) {
        Timber.e("Entry is still locked. Fail unlock");
        return false;
      }

      final String appCode = padLockEntry.lockCode();
      String pin;
      if (appCode == null) {
        Timber.d("No app specific code, use Master PIN");
        pin = masterPin;
      } else {
        Timber.d("App specific code present, compare attempt");
        pin = appCode;
      }

      final boolean unlocked = checkSubmissionAttempt(attempt, pin);

      // KLUDGE we must do this here as we need the padlock entry
      if (unlocked) {
        if (ignoreForPeriod != getIgnoreTimeNone()) {
          Timber.d("IGNORE requested, update entry in DB");
          ignoreEntryForTime(padLockEntry, ignoreForPeriod);
        }
      }

      return unlocked;
    }).map(unlocked -> {
      if (unlocked) {
        Timber.d("Run finishing unlock hooks");

        if (shouldExclude) {
          Timber.d("EXCLUDE requested, delete entry from DB");
          dbInteractor.deleteEntry(packageName, activityName);
        }
      }

      return unlocked;
    });
  }

  @WorkerThread
  private void ignoreEntryForTime(final PadLockEntry oldValues, final long ignoreForPeriod) {
    final long ignoreMinutesInMillis = ignoreForPeriod * 60 * 1000;
    Timber.d("Ignore %s %s for %d", oldValues.packageName(), oldValues.activityName(),
        ignoreMinutesInMillis);
    final ContentValues contentValues = PadLockEntry.FACTORY.marshal()
        .packageName(oldValues.packageName())
        .activityName(oldValues.activityName())
        .lockCode(oldValues.lockCode())
        .lockUntilTime(oldValues.lockUntilTime())
        .ignoreUntilTime(System.currentTimeMillis() + ignoreMinutesInMillis)
        .systemApplication(oldValues.systemApplication())
        .asContentValues();
    PadLockOpenHelper.updateWithPackageActivityName(appContext, contentValues,
        oldValues.packageName(), oldValues.activityName());
  }

  @CheckResult @Override public long getDefaultIgnoreTime() {
    return preferences.getDefaultIgnoreTime();
  }

  @WorkerThread @NonNull @Override @CheckResult
  public Observable<String> getDisplayName(@NonNull String packageName) {
    final PackageManager packageManager = appContext.getPackageManager();
    try {
      final ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
      return Observable.just(applicationInfo.loadLabel(packageManager).toString());
    } catch (PackageManager.NameNotFoundException e) {
      Timber.e(e, "EXCEPTION");
      return Observable.just("");
    }
  }

  @Override public long getIgnoreTimeForIndex(int index) {
    return ignoreTimes[index];
  }

  @CheckResult private long getIgnoreTimeNone() {
    return getIgnoreTimeForIndex(0);
  }

  @Override public long getIgnoreTimeOne() {
    return getIgnoreTimeForIndex(1);
  }

  @Override public long getIgnoreTimeFive() {
    return getIgnoreTimeForIndex(2);
  }

  @Override public long getIgnoreTimeTen() {
    return getIgnoreTimeForIndex(3);
  }

  @Override public long getIgnoreTimeFifteen() {
    return getIgnoreTimeForIndex(4);
  }

  @Override public long getIgnoreTimeTwenty() {
    return getIgnoreTimeForIndex(5);
  }

  @Override public long getIgnoreTimeThirty() {
    return getIgnoreTimeForIndex(6);
  }

  @Override public long getIgnoreTimeFourtyFive() {
    return getIgnoreTimeForIndex(7);
  }

  @Override public long getIgnoreTimeSixty() {
    return getIgnoreTimeForIndex(8);
  }

  @Override public int incrementAndGetFailCount() {
    return ++failCount;
  }

  @Override public void resetFailCount() {
    Timber.d("Reset fail count to 0");
    failCount = 0;
  }
}
