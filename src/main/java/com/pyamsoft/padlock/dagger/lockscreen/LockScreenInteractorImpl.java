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

package com.pyamsoft.padlock.dagger.lockscreen;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.db.DBInteractor;
import com.pyamsoft.padlock.app.lockscreen.LockScreenInteractor;
import com.pyamsoft.padlock.app.pin.MasterPinInteractor;
import com.pyamsoft.padlock.dagger.lock.LockInteractorImpl;
import com.pyamsoft.padlock.model.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

final class LockScreenInteractorImpl extends LockInteractorImpl implements LockScreenInteractor {

  @NonNull private final MasterPinInteractor pinInteractor;
  @NonNull private final DBInteractor dbInteractor;
  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferences preferences;

  @Inject public LockScreenInteractorImpl(final Context context,
      @NonNull final PadLockPreferences preferences, @NonNull final DBInteractor dbInteractor,
      @NonNull final MasterPinInteractor masterPinInteractor) {
    this.appContext = context.getApplicationContext();
    this.preferences = preferences;
    this.dbInteractor = dbInteractor;
    this.pinInteractor = masterPinInteractor;
  }

  @WorkerThread @NonNull @Override
  public Observable<Boolean> lockEntry(String packageName, String activityName) {
    Timber.d("Lock entry: %s %s", packageName, activityName);
    return PadLockDB.with(appContext)
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME, packageName,
            activityName)
        .mapToOne(PadLockEntry.MAPPER::map)
        .filter(padLockEntry -> padLockEntry != null)
        .first()
        .map(padLockEntry -> {
          Timber.d("LOCKED entry, update entry in DB: ", padLockEntry);
          final long timeOutMinutesInMillis = preferences.getTimeoutPeriod() * 60 * 1000;
          final ContentValues contentValues =
              new PadLockEntry.Marshal().packageName(padLockEntry.packageName())
                  .activityName(padLockEntry.activityName())
                  .displayName(padLockEntry.displayName())
                  .lockCode(padLockEntry.lockCode())
                  .lockUntilTime(System.currentTimeMillis() + timeOutMinutesInMillis)
                  .ignoreUntilTime(padLockEntry.ignoreUntilTime())
                  .systemApplication(padLockEntry.systemApplication())
                  .asContentValues();
          PadLockDB.with(appContext)
              .update(PadLockEntry.TABLE_NAME, contentValues,
                  PadLockEntry.UPDATE_WITH_PACKAGE_ACTIVITY_NAME, padLockEntry.packageName(),
                  padLockEntry.activityName());
          return true;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  @WorkerThread @NonNull @Override
  public Observable<Boolean> unlockEntry(String packageName, String activityName, String attempt,
      boolean shouldExclude, long ignoreForPeriod) {
    Timber.d("Attempt unlock: %s %s", packageName, activityName);
    final Observable<PadLockEntry> dbObservable = PadLockDB.with(appContext)
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME, packageName,
            activityName)
        .mapToOne(PadLockEntry.MAPPER::map)
        .filter(padLockEntry -> padLockEntry != null)
        .first()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());

    final Observable<String> masterPinObservable =
        Observable.defer(() -> Observable.just(pinInteractor.getMasterPin()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());

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
        if (ignoreForPeriod != PadLockPreferences.PERIOD_NONE) {
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
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
  }

  @WorkerThread
  private void ignoreEntryForTime(final PadLockEntry oldValues, final long ignoreForPeriod) {
    final long ignoreMinutesInMillis = ignoreForPeriod * 60 * 1000;
    final ContentValues contentValues =
        new PadLockEntry.Marshal().packageName(oldValues.packageName())
            .activityName(oldValues.activityName())
            .displayName(oldValues.displayName())
            .lockCode(oldValues.lockCode())
            .lockUntilTime(oldValues.lockUntilTime())
            .ignoreUntilTime(System.currentTimeMillis() + ignoreMinutesInMillis)
            .systemApplication(oldValues.systemApplication())
            .asContentValues();
    PadLockDB.with(appContext)
        .update(PadLockEntry.TABLE_NAME, contentValues,
            PadLockEntry.UPDATE_WITH_PACKAGE_ACTIVITY_NAME, oldValues.packageName(),
            oldValues.activityName());
  }

  @WorkerThread @NonNull @Override public Observable<Long> getDefaultIgnoreTime() {
    return Observable.defer(() -> Observable.just(preferences.getDefaultIgnoreTime()))
        .map(aLong -> aLong == null ? PadLockPreferences.PERIOD_NONE : aLong);
  }

  @WorkerThread @NonNull @Override public Observable<Long> setDefaultIgnoreTime(long ignoreTime) {
    return Observable.defer(() -> {
      preferences.setDefaultIgnoreTime(ignoreTime);
      return Observable.just(ignoreTime);
    });
  }
}
