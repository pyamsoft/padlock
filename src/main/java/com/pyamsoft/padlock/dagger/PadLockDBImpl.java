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

package com.pyamsoft.padlock.dagger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroidrx.SubscriptionHelper;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import timber.log.Timber;

class PadLockDBImpl implements PadLockDB {

  @SuppressWarnings("WeakerAccess") @NonNull final BriteDatabase briteDatabase;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockOpenHelper openHelper;
  @NonNull private final Scheduler dbScheduler;
  @SuppressWarnings("WeakerAccess") @Nullable Subscription dbOpenSubscription;

  @Inject PadLockDBImpl(@NonNull Context context, @NonNull Scheduler scheduler) {
    dbScheduler = scheduler;
    openHelper = new PadLockOpenHelper(context);
    briteDatabase = new SqlBrite.Builder().build().wrapDatabaseHelper(openHelper, scheduler);
  }

  @SuppressWarnings("WeakerAccess") void openDatabase() {
    SubscriptionHelper.unsubscribe(dbOpenSubscription);

    // After a 1 minute timeout, close the DB
    dbOpenSubscription = Observable.timer(1, TimeUnit.MINUTES)
        .map(aLong -> {
          briteDatabase.close();
          return true;
        })
        .subscribeOn(dbScheduler)
        .observeOn(dbScheduler)
        .subscribe(ignoreMe -> Timber.d("PadLockDB is closed"),
            throwable -> Timber.e(throwable, "onError closing database"),
            () -> SubscriptionHelper.unsubscribe(dbOpenSubscription));
  }

  @Override @CheckResult @NonNull
  public Observable<Long> insert(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, long ignoreUntilTime, boolean isSystem,
      boolean whitelist) {
    final PadLockEntry entry =
        PadLockEntry.CREATOR.create(packageName, activityName, lockCode, lockUntilTime,
            ignoreUntilTime, isSystem, whitelist);
    if (PadLockEntry.isEmpty(entry)) {
      throw new RuntimeException("Cannot insert EMPTY entry");
    }

    return Observable.defer(() -> {
      Timber.i("DB: INSERT");
      openDatabase();
      return deleteWithPackageActivityNameUnguarded(packageName, activityName);
    }).map(deleted -> {
      Timber.d("Delete result: %d", deleted);
      return PadLockEntry.insertEntry(openHelper).executeProgram(entry);
    });
  }

  @NonNull @Override
  public Observable<Integer> updateIgnoreTime(long ignoreUntilTime, @NonNull String packageName,
      @NonNull String activityName) {
    if (packageName.equals(PadLockEntry.PACKAGE_EMPTY) || activityName.equals(
        PadLockEntry.ACTIVITY_EMPTY)) {
      throw new RuntimeException("Cannot update EMPTY entry");
    }

    return Observable.defer(() -> {
      Timber.i("DB: UPDATE IGNORE");
      openDatabase();
      return Observable.fromCallable(() -> PadLockEntry.updateIgnoreTime(openHelper)
          .executeProgram(ignoreUntilTime, packageName, activityName));
    });
  }

  @NonNull @Override
  public Observable<Integer> updateLockTime(long lockUntilTime, @NonNull String packageName,
      @NonNull String activityName) {
    if (packageName.equals(PadLockEntry.PACKAGE_EMPTY) || activityName.equals(
        PadLockEntry.ACTIVITY_EMPTY)) {
      throw new RuntimeException("Cannot update EMPTY entry");
    }

    return Observable.defer(() -> {
      Timber.i("DB: UPDATE LOCK");
      openDatabase();
      return Observable.fromCallable(() -> PadLockEntry.updateLockTime(openHelper)
          .executeProgram(lockUntilTime, packageName, activityName));
    });
  }

  @NonNull @Override
  public Observable<Integer> updateWhitelist(boolean whitelist, @NonNull String packageName,
      @NonNull String activityName) {
    if (packageName.equals(PadLockEntry.PACKAGE_EMPTY) || activityName.equals(
        PadLockEntry.ACTIVITY_EMPTY)) {
      throw new RuntimeException("Cannot update EMPTY entry");
    }

    return Observable.defer(() -> {
      Timber.i("DB: UPDATE WHITELIST");
      openDatabase();
      return Observable.fromCallable(() -> PadLockEntry.updateWhitelist(openHelper)
          .executeProgram(whitelist, packageName, activityName));
    });
  }

  @Override @NonNull @CheckResult
  public Observable<PadLockEntry.WithPackageActivityName> queryWithPackageActivityName(
      final @NonNull String packageName, final @NonNull String activityName) {
    return Observable.defer(() -> {
      Timber.i("DB: QUERY PACKAGE ACTIVITY");
      openDatabase();
      return briteDatabase.createQuery(PadLockEntry.TABLE_NAME,
          PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName)
          .mapToOneOrDefault(PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME_MAPPER::map,
              PadLockEntry.WithPackageActivityName.empty());
    });
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
  @Override @NonNull @CheckResult
  public Observable<PadLockEntry> queryWithPackageActivityNameDefault(
      final @NonNull String packageName, final @NonNull String activityName) {
    return Observable.defer(() -> {
      Timber.i("DB: QUERY PACKAGE ACTIVITY DEFAULT");
      openDatabase();
      return briteDatabase.createQuery(PadLockEntry.TABLE_NAME,
          PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME_DEFAULT, packageName,
          PadLockEntry.PACKAGE_ACTIVITY_NAME, activityName, PadLockEntry.PACKAGE_ACTIVITY_NAME,
          activityName)
          .mapToOneOrDefault(PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME_DEFAULT_MAPPER::map,
              PadLockEntry.empty());
    });
  }

  @Override @NonNull @CheckResult
  public Observable<List<PadLockEntry.WithPackageName>> queryWithPackageName(
      final @NonNull String packageName) {
    return Observable.defer(() -> {
      Timber.i("DB: QUERY PACKAGE");
      openDatabase();
      return briteDatabase.createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_NAME,
          packageName).mapToList(PadLockEntry.WITH_PACKAGE_NAME_MAPPER::map);
    });
  }

  @Override @NonNull @CheckResult public Observable<List<PadLockEntry.AllEntries>> queryAll() {
    return Observable.defer(() -> {
      Timber.i("DB: QUERY ALL");
      openDatabase();
      return briteDatabase.createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
          .mapToList(PadLockEntry.ALL_ENTRIES_MAPPER::map);
    });
  }

  @Override @NonNull @CheckResult
  public Observable<Integer> deleteWithPackageName(final @NonNull String packageName) {
    return Observable.defer(() -> {
      Timber.i("DB: DELETE PACKAGE");
      openDatabase();
      return Observable.fromCallable(
          () -> PadLockEntry.deletePackage(openHelper).executeProgram(packageName));
    });
  }

  @Override @NonNull @CheckResult
  public Observable<Integer> deleteWithPackageActivityName(final @NonNull String packageName,
      final @NonNull String activityName) {
    return Observable.defer(() -> {
      Timber.i("DB: DELETE PACKAGE ACTIVITY");
      openDatabase();
      return deleteWithPackageActivityNameUnguarded(packageName, activityName);
    });
  }

  @VisibleForTesting @NonNull @CheckResult
  Observable<Integer> deleteWithPackageActivityNameUnguarded(@NonNull String packageName,
      @NonNull String activityName) {
    return Observable.just(
        PadLockEntry.deletePackageActivity(openHelper).executeProgram(packageName, activityName));
  }

  @Override @NonNull @CheckResult public Observable<Integer> deleteAll() {
    return Observable.defer(() -> {
      Timber.i("DB: DELETE ALL");
      briteDatabase.execute(PadLockEntry.DELETE_ALL);
      SubscriptionHelper.unsubscribe(dbOpenSubscription);
      briteDatabase.close();
      deleteDatabase();
      return Observable.just(1);
    });
  }

  @Override public void deleteDatabase() {
    openHelper.deleteDatabase();
  }

  @SuppressWarnings("WeakerAccess") static class PadLockOpenHelper extends SQLiteOpenHelper {

    @NonNull private static final String DB_NAME = "padlock_db";
    private static final int DATABASE_VERSION = 4;
    @NonNull private final Context appContext;

    PadLockOpenHelper(final @NonNull Context context) {
      super(context.getApplicationContext(), DB_NAME, null, DATABASE_VERSION);
      appContext = context.getApplicationContext();
    }

    void deleteDatabase() {
      appContext.deleteDatabase(DB_NAME);
    }

    @Override public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
      Timber.d("onCreate");
      Timber.d("EXEC SQL: %s", PadLockEntry.CREATE_TABLE);
      sqLiteDatabase.execSQL(PadLockEntry.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
      Timber.d("onUpgrade from old version %d to new %d", oldVersion, newVersion);
      int currentVersion = oldVersion;
      if (currentVersion == 1 && newVersion >= 2) {
        upgradeVersion1To2(sqLiteDatabase);
        ++currentVersion;
      }

      if (currentVersion == 2 && newVersion >= 3) {
        upgradeVersion2To3(sqLiteDatabase);
        ++currentVersion;
      }

      if (currentVersion == 3 && newVersion >= 4) {
        upgradeVersion3To4(sqLiteDatabase);
        ++currentVersion;
      }
    }

    private void upgradeVersion3To4(SQLiteDatabase sqLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 adds whitelist column");
      final String alterWithWhitelist = String.format(Locale.getDefault(),
          "ALTER TABLE %s ADD COLUMN %S INTEGER NOT NULL DEFAULT 0", PadLockEntry.TABLE_NAME,
          PadLockEntry.WHITELIST);

      Timber.d("EXEC SQL: %s", alterWithWhitelist);
      sqLiteDatabase.execSQL(alterWithWhitelist);
    }

    private void upgradeVersion2To3(SQLiteDatabase sqLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 drops the whole table");

      final String dropOldTable =
          String.format(Locale.getDefault(), "DROP TABLE %s", PadLockEntry.TABLE_NAME);
      Timber.d("EXEC SQL: %s", dropOldTable);
      sqLiteDatabase.execSQL(dropOldTable);

      // Creating the table again
      onCreate(sqLiteDatabase);
    }

    private void upgradeVersion1To2(@NonNull SQLiteDatabase sqLiteDatabase) {
      Timber.d("Upgrading from Version 1 to 2 drops the displayName column");

      // Remove the columns we don't want anymore from the table's list of columns
      Timber.d("Gather a list of the remaining columns");
      final String[] updatedTableColumns = {
          PadLockEntry.PACKAGENAME, PadLockEntry.ACTIVITYNAME, PadLockEntry.LOCKCODE,
          PadLockEntry.LOCKUNTILTIME, PadLockEntry.IGNOREUNTILTIME, PadLockEntry.SYSTEMAPPLICATION
      };

      final String columnsSeperated = TextUtils.join(",", updatedTableColumns);
      Timber.d("Column seperated: %s", columnsSeperated);

      final String tableName = PadLockEntry.TABLE_NAME;
      final String oldTable = tableName + "_old";
      final String alterTable =
          String.format(Locale.getDefault(), "ALTER TABLE %s RENAME TO %s", tableName, oldTable);
      final String insertIntoNewTable =
          String.format(Locale.getDefault(), "INSERT INTO %s(%s) SELECT %s FROM %s", tableName,
              columnsSeperated, columnsSeperated, oldTable);
      final String dropOldTable = String.format(Locale.getDefault(), "DROP TABLE %s", oldTable);

      // Move the existing table to an old table
      Timber.d("EXEC SQL: %s", alterTable);
      sqLiteDatabase.execSQL(alterTable);

      onCreate(sqLiteDatabase);

      // Populating the table with the data
      Timber.d("EXEC SQL: %s", insertIntoNewTable);
      sqLiteDatabase.execSQL(insertIntoNewTable);

      Timber.d("EXEC SQL: %s", dropOldTable);
      sqLiteDatabase.execSQL(dropOldTable);
    }
  }
}
