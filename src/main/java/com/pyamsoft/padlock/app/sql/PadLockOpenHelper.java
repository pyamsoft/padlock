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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.squareup.sqlbrite.BriteDatabase;
import java.util.List;
import java.util.Locale;
import rx.Observable;
import timber.log.Timber;

public final class PadLockOpenHelper extends SQLiteOpenHelper {

  private static final int DATABASE_VERSION = 2;

  public PadLockOpenHelper(final @NonNull Context context) {
    super(context.getApplicationContext(), "padlock_db", null, DATABASE_VERSION);
  }

  private static void upgradeVersion1To2(@NonNull SQLiteDatabase sqLiteDatabase) {
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

    // Creating the table on its new format (no redundant columns)
    Timber.d("EXEC SQL: %s", PadLockEntry.CREATE_TABLE);
    sqLiteDatabase.execSQL(PadLockEntry.CREATE_TABLE);

    // Populating the table with the data
    Timber.d("EXEC SQL: %s", insertIntoNewTable);
    sqLiteDatabase.execSQL(insertIntoNewTable);

    Timber.d("EXEC SQL: %s", dropOldTable);
    sqLiteDatabase.execSQL(dropOldTable);
  }

  @SuppressLint("NewApi") public static void newTransaction(final @NonNull Context context,
      final @NonNull Runnable runnable) {
    final Context appContext = context.getApplicationContext();
    try (
        final BriteDatabase.Transaction transaction = PadLockDB.with(appContext).newTransaction()) {
      runnable.run();
      transaction.markSuccessful();
    }
  }

  public static void insert(final @NonNull Context context,
      final @NonNull ContentValues contentValues) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext).insert(PadLockEntry.TABLE_NAME, contentValues);
  }

  @NonNull @CheckResult public static Observable<PadLockEntry> queryWithPackageActivityName(
      final @NonNull Context context, final @NonNull String packageName,
      final @NonNull String activityName) {
    final Context appContext = context.getApplicationContext();
    return PadLockDB.with(appContext)
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME, packageName,
            activityName)
        .mapToOne(PadLockEntry.MAPPER::map)
        .filter(padLockEntry -> padLockEntry != null);
  }

  @NonNull @CheckResult
  public static Observable<List<PadLockEntry>> queryWithPackageName(final @NonNull Context context,
      final @NonNull String packageName) {
    final Context appContext = context.getApplicationContext();
    return PadLockDB.with(appContext)
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_NAME, packageName)
        .mapToList(PadLockEntry.MAPPER::map)
        .filter(padLockEntries -> padLockEntries != null);
  }

  public static void updateWithPackageActivityName(final @NonNull Context context,
      final @NonNull ContentValues contentValues, final @NonNull String packageName,
      final @NonNull String activityName) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext)
        .update(PadLockEntry.TABLE_NAME, contentValues,
            PadLockEntry.UPDATE_WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName);
  }

  @NonNull @CheckResult
  public static Observable<List<PadLockEntry>> queryAll(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    return PadLockDB.with(appContext)
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
        .mapToList(PadLockEntry.MAPPER::map)
        .filter(padLockEntries -> padLockEntries != null);
  }

  public static void deleteWithPackageName(final @NonNull Context context,
      final @NonNull String packageName) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext)
        .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_NAME, packageName);
  }

  public static void deleteWithPackageActivityName(final @NonNull Context context,
      final @NonNull String packageName, final @NonNull String activityName) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext)
        .delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_WITH_PACKAGE_ACTIVITY_NAME,
            packageName, activityName);
  }

  public static void deleteAll(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext).delete(PadLockEntry.TABLE_NAME, PadLockEntry.DELETE_ALL);
  }

  @Override public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
    Timber.d("onCreate");
    sqLiteDatabase.execSQL(PadLockEntry.CREATE_TABLE);
  }

  @Override
  public void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    Timber.d("onUpgrade from old version %d to new %d", oldVersion, newVersion);
    if (oldVersion == 1 && newVersion == 2) {
      upgradeVersion1To2(sqLiteDatabase);
    }
  }
}
