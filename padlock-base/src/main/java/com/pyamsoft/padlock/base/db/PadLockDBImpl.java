/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.base.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;
import com.squareup.sqldelight.SqlDelightStatement;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import timber.log.Timber;

class PadLockDBImpl implements PadLockDB {

  @SuppressWarnings("WeakerAccess") @NonNull final BriteDatabase briteDatabase;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockOpenHelper openHelper;

  @Inject PadLockDBImpl(@NonNull Context context, @NonNull Scheduler scheduler) {
    openHelper = new PadLockOpenHelper(context);
    briteDatabase = new SqlBrite.Builder().build().wrapDatabaseHelper(openHelper, scheduler);
  }

  @Override @CheckResult @NonNull
  public Completable insert(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, long ignoreUntilTime, boolean isSystem,
      boolean whitelist) {
    return Single.fromCallable(() -> {
      final PadLockEntry entry =
          PadLockEntry.create(packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime,
              isSystem, whitelist);
      if (PadLockEntry.isEmpty(entry)) {
        throw new RuntimeException("Cannot insert EMPTY entry");
      }

      Timber.i("DB: INSERT");
      final int deleteResult = deleteWithPackageActivityNameUnguarded(packageName, activityName);
      Timber.d("Delete result: %d", deleteResult);
      return entry;
    })
        .flatMapCompletable(padLockEntry -> Completable.fromCallable(
            () -> PadLockEntry.insertEntry(openHelper).executeProgram(padLockEntry)));
  }

  @NonNull @Override
  public Completable updateIgnoreTime(long ignoreUntilTime, @NonNull String packageName,
      @NonNull String activityName) {
    return Completable.fromCallable(() -> {
      if (PadLockEntry.PACKAGE_EMPTY.equals(packageName) || PadLockEntry.ACTIVITY_EMPTY.equals(
          activityName)) {
        throw new RuntimeException("Cannot update EMPTY entry");
      }

      Timber.i("DB: UPDATE IGNORE");
      return PadLockEntry.updateIgnoreTime(openHelper)
          .executeProgram(ignoreUntilTime, packageName, activityName);
    });
  }

  @NonNull @Override
  public Completable updateLockTime(long lockUntilTime, @NonNull String packageName,
      @NonNull String activityName) {
    return Completable.fromCallable(() -> {
      if (PadLockEntry.PACKAGE_EMPTY.equals(packageName) || PadLockEntry.ACTIVITY_EMPTY.equals(
          activityName)) {
        throw new RuntimeException("Cannot update EMPTY entry");
      }

      Timber.i("DB: UPDATE LOCK");
      return PadLockEntry.updateLockTime(openHelper)
          .executeProgram(lockUntilTime, packageName, activityName);
    });
  }

  @NonNull @Override
  public Completable updateWhitelist(boolean whitelist, @NonNull String packageName,
      @NonNull String activityName) {
    return Completable.fromCallable(() -> {
      if (PadLockEntry.PACKAGE_EMPTY.equals(packageName) || PadLockEntry.ACTIVITY_EMPTY.equals(
          activityName)) {
        throw new RuntimeException("Cannot update EMPTY entry");
      }

      Timber.i("DB: UPDATE WHITELIST");
      return PadLockEntry.updateWhitelist(openHelper)
          .executeProgram(whitelist, packageName, activityName);
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
  @Override @NonNull @CheckResult public Single<PadLockEntry> queryWithPackageActivityNameDefault(
      final @NonNull String packageName, final @NonNull String activityName) {
    return Single.defer(() -> {
      Timber.i("DB: QUERY PACKAGE ACTIVITY DEFAULT");

      SqlDelightStatement statement =
          PadLockEntry.withPackageActivityNameDefault(packageName, activityName);

      return briteDatabase.createQuery(statement.tables, statement.statement, statement.args)
          .mapToOneOrDefault(PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME_DEFAULT_MAPPER::map,
              PadLockEntry.EMPTY)
          .first(PadLockEntry.EMPTY);
    });
  }

  @Override @NonNull @CheckResult
  public Single<List<PadLockEntry.WithPackageName>> queryWithPackageName(
      @NonNull String packageName) {
    return Single.defer(() -> {
      Timber.i("DB: QUERY PACKAGE");

      SqlDelightStatement statement = PadLockEntry.withPackageName(packageName);
      return briteDatabase.createQuery(statement.tables, statement.statement, statement.args)
          .mapToList(PadLockEntry.WITH_PACKAGE_NAME_MAPPER::map)
          .first(Collections.emptyList());
    });
  }

  @Override @NonNull @CheckResult public Single<List<PadLockEntry.AllEntries>> queryAll() {
    return Single.defer(() -> {
      Timber.i("DB: QUERY ALL");

      SqlDelightStatement statement = PadLockEntry.queryAll();
      return briteDatabase.createQuery(statement.tables, statement.statement, statement.args)
          .mapToList(PadLockEntry.ALL_ENTRIES_MAPPER::map)
          .first(Collections.emptyList());
    });
  }

  @Override @NonNull @CheckResult
  public Completable deleteWithPackageName(final @NonNull String packageName) {
    return Completable.fromCallable(() -> {
      Timber.i("DB: DELETE PACKAGE");
      return PadLockEntry.deletePackage(openHelper).executeProgram(packageName);
    });
  }

  @Override @NonNull @CheckResult
  public Completable deleteWithPackageActivityName(final @NonNull String packageName,
      final @NonNull String activityName) {
    return Completable.fromCallable(() -> {
      Timber.i("DB: DELETE PACKAGE ACTIVITY");
      return deleteWithPackageActivityNameUnguarded(packageName, activityName);
    });
  }

  @SuppressWarnings("WeakerAccess") @VisibleForTesting @CheckResult
  int deleteWithPackageActivityNameUnguarded(@NonNull String packageName,
      @NonNull String activityName) {
    return PadLockEntry.deletePackageActivity(openHelper).executeProgram(packageName, activityName);
  }

  @Override @NonNull @CheckResult public Completable deleteAll() {
    return Completable.fromAction(() -> {
      Timber.i("DB: DELETE ALL");
      briteDatabase.execute(PadLockEntry.DELETE_ALL);
      briteDatabase.close();
    }).andThen(deleteDatabase());
  }

  @NonNull @Override public Completable deleteDatabase() {
    return Completable.fromAction(openHelper::deleteDatabase);
  }

  private static class PadLockOpenHelper extends SQLiteOpenHelper {

    @NonNull private static final String DB_NAME = "padlock_db";
    private static final int DATABASE_VERSION = 4;
    @NonNull private static final String[] UPGRADE_1_TO_2_TABLE_COLUMNS = {
        PadLockEntry.PACKAGENAME, PadLockEntry.ACTIVITYNAME, PadLockEntry.LOCKCODE,
        PadLockEntry.LOCKUNTILTIME, PadLockEntry.IGNOREUNTILTIME, PadLockEntry.SYSTEMAPPLICATION
    };
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
      final String columnsSeperated = TextUtils.join(",", UPGRADE_1_TO_2_TABLE_COLUMNS);
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
