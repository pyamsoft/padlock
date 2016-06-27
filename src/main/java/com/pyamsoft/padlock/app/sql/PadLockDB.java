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

package com.pyamsoft.padlock.app.sql;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import java.util.List;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public final class PadLockDB {

  @NonNull private final BriteDatabase briteDatabase;
  private static volatile Delegate instance = null;

  PadLockDB(final @NonNull Context context, final @NonNull Scheduler dbScheduler) {
    final SqlBrite sqlBrite = SqlBrite.create();
    final PadLockOpenHelper openHelper = new PadLockOpenHelper(context.getApplicationContext());
    briteDatabase = sqlBrite.wrapDatabaseHelper(openHelper, dbScheduler);
  }

  @NonNull @CheckResult public final BriteDatabase getDatabase() {
    return briteDatabase;
  }

  public static void setDelegate(@NonNull Delegate delegate) {
    instance = delegate;
  }

  @CheckResult @NonNull public static Delegate with(@NonNull Context context) {
    return with(context, Schedulers.io());
  }

  @CheckResult @NonNull
  public static Delegate with(@NonNull Context context, @NonNull Scheduler scheduler) {
    if (instance == null) {
      synchronized (Delegate.class) {
        if (instance == null) {
          instance = new Delegate(context, scheduler);
        }
      }
    }

    return instance;
  }

  public static final class Delegate {

    @NonNull private final PadLockDB database;

    Delegate(@NonNull Context context) {
      this(context, Schedulers.io());
    }

    Delegate(@NonNull Context context, @NonNull Scheduler scheduler) {
      final Context appContext = context.getApplicationContext();
      this.database = new PadLockDB(appContext, scheduler);
    }

    @SuppressLint("NewApi") public final void newTransaction(final @NonNull Runnable runnable) {
      try (
          final BriteDatabase.Transaction transaction = database.getDatabase().newTransaction()) {
        runnable.run();
        transaction.markSuccessful();
      }
    }

    public final void insert(final @NonNull ContentValues contentValues) {
      database.getDatabase().insert(PadLockEntry.TABLE_NAME, contentValues);
    }

    @NonNull @CheckResult public final Observable<PadLockEntry> queryWithPackageActivityName(
        final @NonNull String packageName, final @NonNull String activityName) {
      return database.getDatabase()
          .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME,
              packageName, activityName)
          .mapToOneOrDefault(PadLockEntry.FACTORY.with_package_activity_nameMapper()::map,
              PadLockEntry.empty())
          .filter(padLockEntry -> padLockEntry != null);
    }

    @NonNull @CheckResult public final Observable<List<PadLockEntry>> queryWithPackageName(
        final @NonNull String packageName) {
      return database.getDatabase()
          .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_NAME, packageName)
          .mapToList(PadLockEntry.FACTORY.with_package_nameMapper()::map)
          .filter(padLockEntries -> padLockEntries != null);
    }

    public final void updateWithPackageActivityName(final @NonNull ContentValues contentValues,
        final @NonNull String packageName, final @NonNull String activityName) {
      database.getDatabase()
          .update(PadLockEntry.TABLE_NAME, contentValues,
              PadLockEntry.UPDATE_WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName);
    }

    @NonNull @CheckResult public final Observable<List<PadLockEntry>> queryAll() {
      return database.getDatabase()
          .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
          .mapToList(PadLockEntry.FACTORY.all_entriesMapper()::map)
          .filter(padLockEntries -> padLockEntries != null);
    }

    public final void deleteWithPackageName(final @NonNull String packageName) {
      database.getDatabase()
          .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_NAME, packageName);
    }

    public final void deleteWithPackageActivityName(final @NonNull String packageName,
        final @NonNull String activityName) {
      database.getDatabase()
          .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_ACTIVITY_NAME,
              packageName, activityName);
    }

    public final void deleteAll() {
      database.getDatabase().delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_ALL);
    }
  }
}
