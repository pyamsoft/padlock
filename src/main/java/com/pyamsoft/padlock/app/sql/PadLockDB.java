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
import android.support.annotation.VisibleForTesting;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class PadLockDB {

  @NonNull private static final Object lock = new Object();
  private static volatile PadLockDB instance = null;
  @SuppressWarnings("WeakerAccess") @NonNull final BriteDatabase briteDatabase;
  @NonNull private final AtomicInteger openCount;

  private PadLockDB(final @NonNull Context context, final @NonNull Scheduler scheduler) {
    briteDatabase = SqlBrite.create().wrapDatabaseHelper(new PadLockOpenHelper(context), scheduler);
    openCount = new AtomicInteger(0);
  }

  @VisibleForTesting static void setDB(@Nullable PadLockDB delegate) {
    instance = delegate;
  }

  @CheckResult @NonNull public static PadLockDB with(@NonNull Context context) {
    return with(context, Schedulers.io());
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  public static PadLockDB with(@NonNull Context context, @NonNull Scheduler scheduler) {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new PadLockDB(context, scheduler);
        }
      }
    }

    return instance;
  }

  @SuppressWarnings("WeakerAccess") synchronized void openDatabase() {
    Timber.d("Increment open count to: %d", openCount.incrementAndGet());
  }

  @SuppressWarnings("WeakerAccess") synchronized void closeDatabase() {
    Timber.d("Decrement open count to: %d", openCount.decrementAndGet());

    if (openCount.get() == 0) {
      close();
    }
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

    Timber.i("DB: INSERT");
    return deleteWithPackageActivityName(packageName, activityName).map(deleted -> {
      Timber.d("Delete result: %d", deleted);

      openDatabase();
      final long result = briteDatabase.insert(PadLockEntry.TABLE_NAME,
          PadLockEntry.FACTORY.marshal(entry).asContentValues());
      closeDatabase();
      return result;
    });
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

    Timber.i("DB: UPDATE");
    openDatabase();
    return Observable.defer(() -> {
      final int result = briteDatabase.update(PadLockEntry.TABLE_NAME,
          PadLockEntry.FACTORY.marshal(entry).asContentValues(),
          PadLockEntry.UPDATE_WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName);
      closeDatabase();
      return Observable.just(result);
    });
  }

  @NonNull @CheckResult
  public Observable<PadLockEntry> queryWithPackageActivityName(final @NonNull String packageName,
      final @NonNull String activityName) {
    Timber.i("DB: QUERY");
    openDatabase();
    return briteDatabase.createQuery(PadLockEntry.TABLE_NAME,
        PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName)
        .mapToOneOrDefault(PadLockEntry.FACTORY.with_package_activity_nameMapper()::map,
            PadLockEntry.empty())
        .map(entry -> {
          closeDatabase();
          return entry;
        })
        .filter(padLockEntry -> padLockEntry != null);
  }

  /**
   * Get either the package with specific name of the PACKAGE entry
   *
   * SQLite only has bindings so we must make do
   * ?1 package name
   * ?2 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?3 the specific activity name
   * ?4 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?5 the specific activity name
   */
  @NonNull @CheckResult public Observable<PadLockEntry> queryWithPackageActivityNameDefault(
      final @NonNull String packageName, final @NonNull String activityName) {
    Timber.i("DB: QUERY");
    openDatabase();
    return briteDatabase.createQuery(PadLockEntry.TABLE_NAME,
        PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME_DEFAULT, packageName,
        PadLockEntry.PACKAGE_ACTIVITY_NAME, activityName, PadLockEntry.PACKAGE_ACTIVITY_NAME,
        activityName)
        .mapToOneOrDefault(PadLockEntry.FACTORY.with_package_activity_name_defaultMapper()::map,
            PadLockEntry.empty())
        .map(entry -> {
          closeDatabase();
          return entry;
        })
        .filter(padLockEntry -> padLockEntry != null);
  }

  @NonNull @CheckResult
  public Observable<List<PadLockEntry>> queryWithPackageName(final @NonNull String packageName) {
    Timber.i("DB: QUERY");
    openDatabase();
    return briteDatabase.createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_NAME,
        packageName)
        .mapToList(PadLockEntry.FACTORY.with_package_nameMapper()::map)
        .map(padLockEntries -> {
          closeDatabase();
          return padLockEntries;
        })
        .filter(padLockEntries -> padLockEntries != null);
  }

  @NonNull @CheckResult public Observable<List<PadLockEntry>> queryAll() {
    openDatabase();

    return briteDatabase.createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
        .mapToList(PadLockEntry.FACTORY.all_entriesMapper()::map)
        .map(padLockEntries -> {
          closeDatabase();
          return padLockEntries;
        })
        .filter(padLockEntries -> padLockEntries != null);
  }

  @NonNull @CheckResult
  public Observable<Integer> deleteWithPackageName(final @NonNull String packageName) {
    Timber.i("DB: DELETE");
    openDatabase();
    return Observable.defer(() -> {
      final int result =
          briteDatabase.delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_NAME,
              packageName);
      closeDatabase();
      return Observable.just(result);
    });
  }

  @NonNull @CheckResult
  public Observable<Integer> deleteWithPackageActivityName(final @NonNull String packageName,
      final @NonNull String activityName) {
    Timber.i("DB: DELETE");
    openDatabase();
    return Observable.defer(() -> {
      final int result = briteDatabase.delete(PadLockEntry.TABLE_NAME,
          PadLockEntry.DELETE_WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName);
      closeDatabase();
      return Observable.just(result);
    });
  }

  @NonNull @CheckResult public Observable<Integer> deleteAll() {
    Timber.i("DB: DELETE");
    openDatabase();
    return Observable.defer(() -> {
      final int result = briteDatabase.delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_ALL);
      closeDatabase();
      return Observable.just(result);
    });
  }

  public void close() {
    Timber.d("Close and recycle database connection");
    openCount.set(0);
    briteDatabase.close();
  }
}
