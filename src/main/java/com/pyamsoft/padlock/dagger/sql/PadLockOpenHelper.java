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

package com.pyamsoft.padlock.dagger.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.Locale;
import timber.log.Timber;

public class PadLockOpenHelper extends SQLiteOpenHelper {

  @NonNull public static final String DB_NAME = "padlock_db";
  private static final int DATABASE_VERSION = 4;

  public PadLockOpenHelper(final @NonNull Context context) {
    super(context.getApplicationContext(), DB_NAME, null, DATABASE_VERSION);
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
