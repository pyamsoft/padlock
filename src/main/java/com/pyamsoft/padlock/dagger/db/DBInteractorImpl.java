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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.app.db.DBInteractor;
import com.pyamsoft.padlock.model.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.squareup.sqlbrite.BriteDatabase;
import javax.inject.Inject;
import timber.log.Timber;

final class DBInteractorImpl implements DBInteractor {

  @NonNull private final Context appContext;

  @Inject public DBInteractorImpl(final @NonNull Context context) {
    appContext = context.getApplicationContext();
  }

  @WorkerThread @SuppressLint("NewApi") @Override
  public void createEntry(@NonNull String packageName, @NonNull String name, @Nullable String code,
      boolean system) {
    final PackageManager packageManager = appContext.getPackageManager();
    final PackageInfo packageInfo;
    try {
      packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException("PackageManager threw exception: ", e);
    }
    final ActivityInfo[] activities = packageInfo.activities;
    if (activities != null) {
      try (final BriteDatabase.Transaction transaction = PadLockDB.with(appContext)
          .newTransaction()) {
        for (final ActivityInfo info : activities) {
          final String activityName = info.name;
          if (activityName != null) {
            createEntry(packageName, activityName, name, code, system);
          }
        }
        transaction.markSuccessful();
      }
    }
  }

  @WorkerThread @Override
  public void createEntry(@NonNull String packageName, @NonNull String activityName,
      @NonNull String name, @Nullable String code, boolean system) {
    Timber.d("CREATE: %s %s", packageName, activityName);
    PadLockDB.with(appContext)
        .insert(PadLockEntry.TABLE_NAME, new PadLockEntry.Marshal().packageName(packageName)
            .activityName(activityName)
            .displayName(name)
            .lockCode(code)
            .lockUntilTime(0)
            .ignoreUntilTime(0)
            .systemApplication(system)
            .asContentValues());
  }

  @WorkerThread @Override public void deleteEntry(@NonNull String packageName) {
    Timber.d("DELETE: all %s", packageName);
    PadLockDB.with(appContext)
        .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_NAME, packageName);
  }

  @WorkerThread @Override
  public void deleteEntry(@NonNull String packageName, @NonNull String activityName) {
    Timber.d("DELETE: %s %s", packageName, activityName);
    PadLockDB.with(appContext)
        .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_ACTIVITY_NAME,
            packageName, activityName);
  }
}
