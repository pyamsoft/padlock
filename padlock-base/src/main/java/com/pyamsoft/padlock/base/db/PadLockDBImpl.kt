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

package com.pyamsoft.padlock.base.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.CheckResult
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import com.pyamsoft.padlock.service.db.PadLockEntryModel
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

internal class PadLockDBImpl @Inject internal constructor(context: Context,
    scheduler: Scheduler) : PadLockDB {

  protected @JvmField val briteDatabase: BriteDatabase
  protected @JvmField val openHelper: PadLockOpenHelper

  init {
    openHelper = PadLockOpenHelper(context)
    briteDatabase = SqlBrite.Builder().build().wrapDatabaseHelper(openHelper, scheduler)
  }

  @VisibleForTesting @CheckResult protected fun deleteWithPackageActivityNameUnguarded(
      packageName: String,
      activityName: String): Int {
    return PadLockEntry.deletePackageActivity(openHelper).executeProgram(packageName, activityName)
  }

  @CheckResult
  override fun insert(packageName: String, activityName: String,
      lockCode: String?, lockUntilTime: Long, ignoreUntilTime: Long, isSystem: Boolean,
      whitelist: Boolean): Completable {
    return Single.fromCallable {
      val entry = PadLockEntry.create(packageName, activityName, lockCode, lockUntilTime,
          ignoreUntilTime,
          isSystem, whitelist)
      if (PadLockEntry.isEmpty(entry)) {
        throw RuntimeException("Cannot insert EMPTY entry")
      }
      Timber.i("DB: INSERT")
      val deleteResult = deleteWithPackageActivityNameUnguarded(packageName, activityName)
      Timber.d("Delete result: %d", deleteResult)
      return@fromCallable entry
    }.flatMapCompletable {
      Completable.fromCallable {
        PadLockEntry.insertEntry(openHelper).executeProgram(it)
      }
    }
  }

  override fun updateIgnoreTime(ignoreUntilTime: Long, packageName: String,
      activityName: String): Completable {
    return Completable.fromCallable {
      if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
        throw RuntimeException("Cannot update EMPTY entry")
      }
      Timber.i("DB: UPDATE IGNORE")
      return@fromCallable PadLockEntry.updateIgnoreTime(openHelper)
          .executeProgram(ignoreUntilTime, packageName, activityName)
    }
  }

  override fun updateLockTime(lockUntilTime: Long, packageName: String,
      activityName: String): Completable {
    return Completable.fromCallable {
      if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
        throw RuntimeException("Cannot update EMPTY entry")
      }
      Timber.i("DB: UPDATE LOCK")
      return@fromCallable PadLockEntry.updateLockTime(openHelper)
          .executeProgram(lockUntilTime, packageName, activityName)
    }
  }

  override fun updateWhitelist(whitelist: Boolean, packageName: String,
      activityName: String): Completable {
    return Completable.fromCallable {
      if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
        throw RuntimeException("Cannot update EMPTY entry")
      }
      Timber.i("DB: UPDATE WHITELIST")
      return@fromCallable PadLockEntry.updateWhitelist(openHelper)
          .executeProgram(whitelist, packageName, activityName)
    }
  }

  /**
   * Get either the package with specific name of the PACKAGE entry

   * SQLite only has bindings so we must make do
   * ?1 package name
   * ?2 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?3 the specific activity name
   * ?4 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?5 the specific activity name
   */
  @CheckResult override fun queryWithPackageActivityNameDefault(
      packageName: String, activityName: String): Single<PadLockEntry> {
    return Single.defer {
      Timber.i("DB: QUERY PACKAGE ACTIVITY DEFAULT")
      val statement = PadLockEntry.withPackageActivityNameDefault(packageName, activityName)
      return@defer briteDatabase.createQuery(statement.tables, statement.statement, *statement.args)
          .mapToOne { PadLockEntry.WITH_PACKAGE_ACTIVITY_NAME_DEFAULT_MAPPER.map(it) }
          .first(PadLockEntry.EMPTY)
    }
  }

  @CheckResult
  override fun queryWithPackageName(
      packageName: String): Single<List<PadLockEntry.WithPackageName>> {
    return Single.defer {
      Timber.i("DB: QUERY PACKAGE")
      val statement = PadLockEntry.withPackageName(packageName)
      return@defer briteDatabase.createQuery(statement.tables, statement.statement, *statement.args)
          .mapToList { PadLockEntry.WITH_PACKAGE_NAME_MAPPER.map(it) }
          .first(emptyList())
    }
  }

  @CheckResult override fun queryAll(): Single<List<PadLockEntry.AllEntries>> {
    return Single.defer {
      Timber.i("DB: QUERY ALL")
      val statement = PadLockEntry.queryAll()
      return@defer briteDatabase.createQuery(statement.tables, statement.statement, *statement.args)
          .mapToList { PadLockEntry.ALL_ENTRIES_MAPPER.map(it) }
          .first(emptyList())
    }
  }

  @CheckResult
  override fun deleteWithPackageName(packageName: String): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE")
      return@fromCallable PadLockEntry.deletePackage(openHelper).executeProgram(packageName)
    }
  }

  @CheckResult
  override fun deleteWithPackageActivityName(packageName: String,
      activityName: String): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE ACTIVITY")
      return@fromCallable deleteWithPackageActivityNameUnguarded(packageName, activityName)
    }
  }

  @CheckResult override fun deleteAll(): Completable {
    return Completable.fromAction {
      Timber.i("DB: DELETE ALL")
      briteDatabase.execute(PadLockEntryModel.DELETE_ALL)
      briteDatabase.close()
    }.andThen(deleteDatabase())
  }

  override fun deleteDatabase(): Completable {
    return Completable.fromAction({ openHelper.deleteDatabase() })
  }

  internal class PadLockOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(
      context.applicationContext, DB_NAME, null, DATABASE_VERSION) {

    private val appContext: Context = context.applicationContext

    internal fun deleteDatabase() {
      appContext.deleteDatabase(DB_NAME)
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("onCreate")
      Timber.d("EXEC SQL: %s", PadLockEntryModel.CREATE_TABLE)
      sqLiteDatabase.execSQL(PadLockEntryModel.CREATE_TABLE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      Timber.d("onUpgrade from old version %d to new %d", oldVersion, newVersion)
      var currentVersion = oldVersion
      if (currentVersion == 1 && newVersion >= 2) {
        upgradeVersion1To2(sqLiteDatabase)
        ++currentVersion
      }

      if (currentVersion == 2 && newVersion >= 3) {
        upgradeVersion2To3(sqLiteDatabase)
        ++currentVersion
      }

      if (currentVersion == 3 && newVersion >= 4) {
        upgradeVersion3To4(sqLiteDatabase)
        ++currentVersion
      }
    }

    private fun upgradeVersion3To4(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 adds whitelist column")
      val alterWithWhitelist = String.format(Locale.getDefault(),
          "ALTER TABLE %s ADD COLUMN %S INTEGER NOT NULL DEFAULT 0", PadLockEntryModel.TABLE_NAME,
          PadLockEntryModel.WHITELIST)

      Timber.d("EXEC SQL: %s", alterWithWhitelist)
      sqLiteDatabase.execSQL(alterWithWhitelist)
    }

    private fun upgradeVersion2To3(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 drops the whole table")

      val dropOldTable = String.format(Locale.getDefault(), "DROP TABLE %s",
          PadLockEntryModel.TABLE_NAME)
      Timber.d("EXEC SQL: %s", dropOldTable)
      sqLiteDatabase.execSQL(dropOldTable)

      // Creating the table again
      onCreate(sqLiteDatabase)
    }

    private fun upgradeVersion1To2(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("Upgrading from Version 1 to 2 drops the displayName column")

      // Remove the columns we don't want anymore from the table's list of columns
      Timber.d("Gather a list of the remaining columns")
      val columnsSeperated = TextUtils.join(",", UPGRADE_1_TO_2_TABLE_COLUMNS)
      Timber.d("Column seperated: %s", columnsSeperated)

      val tableName = PadLockEntryModel.TABLE_NAME
      val oldTable = tableName + "_old"
      val alterTable = String.format(Locale.getDefault(), "ALTER TABLE %s RENAME TO %s", tableName,
          oldTable)
      val insertIntoNewTable = String.format(Locale.getDefault(),
          "INSERT INTO %s(%s) SELECT %s FROM %s", tableName,
          columnsSeperated, columnsSeperated, oldTable)
      val dropOldTable = String.format(Locale.getDefault(), "DROP TABLE %s", oldTable)

      // Move the existing table to an old table
      Timber.d("EXEC SQL: %s", alterTable)
      sqLiteDatabase.execSQL(alterTable)

      onCreate(sqLiteDatabase)

      // Populating the table with the data
      Timber.d("EXEC SQL: %s", insertIntoNewTable)
      sqLiteDatabase.execSQL(insertIntoNewTable)

      Timber.d("EXEC SQL: %s", dropOldTable)
      sqLiteDatabase.execSQL(dropOldTable)
    }

    companion object {

      const private val DB_NAME = "padlock_db"
      const private val DATABASE_VERSION = 4
      private val UPGRADE_1_TO_2_TABLE_COLUMNS = arrayOf(PadLockEntryModel.PACKAGENAME,
          PadLockEntryModel.ACTIVITYNAME, PadLockEntryModel.LOCKCODE,
          PadLockEntryModel.LOCKUNTILTIME,
          PadLockEntryModel.IGNOREUNTILTIME, PadLockEntryModel.SYSTEMAPPLICATION)
    }
  }

}

