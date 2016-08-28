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

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import java.util.List;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class PadLockDB {

  static volatile Delegate instance = null;
  @NonNull final BriteDatabase briteDatabase;

  PadLockDB(final @NonNull Context context, final @NonNull Scheduler dbScheduler) {
    final SqlBrite sqlBrite = SqlBrite.create();
    final PadLockOpenHelper openHelper = new PadLockOpenHelper(context.getApplicationContext());
    briteDatabase = sqlBrite.wrapDatabaseHelper(openHelper, dbScheduler);
  }

  public static void setDelegate(@Nullable Delegate delegate) {
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

  @NonNull @CheckResult public final BriteDatabase getDatabase() {
    return briteDatabase;
  }

  public static class Delegate {

    @NonNull final PadLockDB database;

    public Delegate(@NonNull Context context, @NonNull Scheduler scheduler) {
      Timber.d("Create new PadLockDB Delegate");
      final Context appContext = context.getApplicationContext();
      this.database = new PadLockDB(appContext, scheduler);
    }

    @CheckResult @NonNull
    public Observable<Long> insert(@NonNull String packageName, @NonNull String activityName,
        @Nullable String lockCode, long lockUntilTime, long ignoreUntilTime, boolean isSystem,
        boolean whitelist) {
      final PadLockEntry entry =
          PadLockEntry.FACTORY.creator.create(packageName, activityName, lockCode, lockUntilTime,
              ignoreUntilTime, isSystem, whitelist);
      if (PadLockEntry.isEmpty(entry)) {
        throw new RuntimeException("Cannot insert EMPTY entry");
      }

      return Observable.defer(() -> Observable.just(database.getDatabase()
          .insert(PadLockEntry.TABLE_NAME, PadLockEntry.FACTORY.marshal(entry).asContentValues())));
    }

    @CheckResult @NonNull
    public Observable<Integer> updateWithPackageActivityName(@NonNull String packageName,
        @NonNull String activityName, @Nullable String lockCode, long lockUntilTime,
        long ignoreUntilTime, boolean isSystem, boolean whitelist) {
      final PadLockEntry entry =
          PadLockEntry.FACTORY.creator.create(packageName, activityName, lockCode, lockUntilTime,
              ignoreUntilTime, isSystem, whitelist);
      if (PadLockEntry.isEmpty(entry)) {
        throw new RuntimeException("Cannot update EMPTY entry");
      }
      return Observable.defer(() -> Observable.just(database.getDatabase()
          .update(PadLockEntry.TABLE_NAME, PadLockEntry.FACTORY.marshal(entry).asContentValues(),
              PadLockEntry.UPDATE_WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName)));
    }

    @NonNull @CheckResult
    public Observable<PadLockEntry> queryWithPackageActivityName(final @NonNull String packageName,
        final @NonNull String activityName) {
      return database.getDatabase()
          .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME,
              packageName, activityName)
          .mapToOneOrDefault(PadLockEntry.FACTORY.with_package_activity_nameMapper()::map,
              PadLockEntry.empty())
          .filter(padLockEntry -> padLockEntry != null);
    }

    @NonNull @CheckResult
    public Observable<List<PadLockEntry>> queryWithPackageName(final @NonNull String packageName) {
      return database.getDatabase()
          .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_NAME, packageName)
          .mapToList(PadLockEntry.FACTORY.with_package_nameMapper()::map)
          .filter(padLockEntries -> padLockEntries != null);
    }

    @NonNull @CheckResult public Observable<List<PadLockEntry>> queryAll() {
      return database.getDatabase()
          .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
          .mapToList(PadLockEntry.FACTORY.all_entriesMapper()::map)
          .filter(padLockEntries -> padLockEntries != null);
    }

    @NonNull @CheckResult
    public Observable<Integer> deleteWithPackageName(final @NonNull String packageName) {
      return Observable.defer(() -> Observable.just(database.getDatabase()
          .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_NAME, packageName)));
    }

    @NonNull @CheckResult
    public Observable<Integer> deleteWithPackageActivityName(final @NonNull String packageName,
        final @NonNull String activityName) {
      return Observable.defer(() -> Observable.just(database.getDatabase()
          .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_ACTIVITY_NAME,
              packageName, activityName)));
    }

    @NonNull @CheckResult public Observable<Integer> deleteAll() {
      return Observable.defer(() -> Observable.just(
          database.getDatabase().delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_ALL)));
    }

    public void close() {
      Timber.d("Close and recycle database connection");
      database.getDatabase().close();
      setDelegate(null);
    }
  }
}
