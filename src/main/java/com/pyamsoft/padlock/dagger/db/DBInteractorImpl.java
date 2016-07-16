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

package com.pyamsoft.padlock.dagger.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.lock.LockScreenActivity1;
import com.pyamsoft.padlock.app.lock.LockScreenActivity2;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.crash.CrashLogActivity;
import com.squareup.sqlbrite.BriteDatabase;
import javax.inject.Inject;
import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

final class DBInteractorImpl implements DBInteractor {

  @NonNull private final Context appContext;

  @Inject public DBInteractorImpl(final @NonNull Context context) {
    appContext = context.getApplicationContext();
  }

  @SuppressLint("NewApi") @NonNull @CheckResult @Override
  public Observable<Long> createActivityEntries(@NonNull String packageName, @Nullable String code,
      boolean system) {
    final PackageManager packageManager = appContext.getPackageManager();
    final PackageInfo packageInfo;
    try {
      packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException("PackageManager threw exception: ", e);
    }

    final ActivityInfo[] activities = packageInfo.activities;
    Observable<Long> result = Observable.empty();
    if (activities != null) {
      try (final BriteDatabase.Transaction transaction = PadLockDB.with(appContext)
          .newTransaction()) {
        for (final ActivityInfo info : activities) {
          final String activityName = info.name;
          if (activityName != null && !activityName.equalsIgnoreCase(
              LockScreenActivity1.class.getName()) && !activityName.equals(
              LockScreenActivity2.class.getName()) && !activityName.equalsIgnoreCase(
              CrashLogActivity.class.getName())) {
            Timber.d("Add entry for %s, %s", packageName, activityName);
            result = result.mergeWith(createEntry(packageName, activityName, code, system));
          }
        }
        Timber.d("Transaction success");
        transaction.markSuccessful();
      }
      // KLUDGE useless result
      Timber.d("Popular result");
      return result;
    } else {
      Timber.d("Useless result");
      return Observable.empty();
    }
  }

  @NonNull @CheckResult @Override
  public Observable<Long> createEntry(@NonNull String packageName, @NonNull String activityName,
      @Nullable String code, boolean system) {

    // To prevent double creations from occurring, first call a delete on the DB for packageName, activityName
    return deleteEntry(packageName, activityName).map(new Func1<Integer, Long>() {
      @Override public Long call(Integer integer) {
        Timber.d("CREATE: %s %s", packageName, activityName);
        return PadLockDB.with(appContext)
            .insert(PadLockEntry.FACTORY.marshal()
                .packageName(packageName)
                .activityName(activityName)
                .lockCode(code)
                .lockUntilTime(0)
                .ignoreUntilTime(0)
                .systemApplication(system)
                .asContentValues());
      }
    });
  }

  @NonNull @Override public Observable<Integer> deleteActivityEntries(@NonNull String packageName) {
    return Observable.defer(() -> {
      Timber.d("DELETE: all %s", packageName);
      return Observable.just(PadLockDB.with(appContext).deleteWithPackageName(packageName));
    });
  }

  @NonNull @Override public Observable<Integer> deleteEntry(@NonNull String packageName,
      @NonNull String activityName) {
    return Observable.defer(() -> {
      Timber.d("DELETE: all %s", packageName);
      return Observable.just(
          PadLockDB.with(appContext).deleteWithPackageActivityName(packageName, activityName));
    });
  }
}
