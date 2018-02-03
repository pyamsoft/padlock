/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.base.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.CheckResult
import android.text.TextUtils
import com.pyamsoft.padlock.api.PadLockDBDelete
import com.pyamsoft.padlock.api.PadLockDBInsert
import com.pyamsoft.padlock.api.PadLockDBQuery
import com.pyamsoft.padlock.api.PadLockDBUpdate
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class PadLockDBImpl @Inject internal constructor(
    context: Context,
    @param:Named("io") private val scheduler: Scheduler
) : PadLockDBInsert,
    PadLockDBUpdate, PadLockDBQuery, PadLockDBDelete {

  private val briteDatabase: BriteDatabase
  private val queryManager: QueryManager
  private val createManager: CreateManager
  private val insertManager: InsertManager
  private val deleteManager: DeleteManager
  private val updateManager: UpdateManager

  init {
    val openHelper = PadLockOpenHelper(context)
    briteDatabase = SqlBrite.Builder()
        .build()
        .wrapDatabaseHelper(openHelper, scheduler)
    queryManager = QueryManager(briteDatabase)
    createManager = CreateManager()
    insertManager = InsertManager(openHelper, briteDatabase)
    deleteManager = DeleteManager(openHelper, briteDatabase)
    updateManager = UpdateManager(openHelper, briteDatabase)
  }

  @CheckResult
  private fun deleteWithPackageActivityNameUnguarded(
      packageName: String,
      activityName: String
  ): Long = deleteManager.deleteWithPackageActivity(
      packageName,
      activityName
  )

  @CheckResult
  override fun insert(
      packageName: String,
      activityName: String,
      lockCode: String?,
      lockUntilTime: Long,
      ignoreUntilTime: Long,
      isSystem: Boolean,
      whitelist: Boolean
  ): Completable {
    return Single.fromCallable {
      val entry = createManager.create(
          packageName, activityName, lockCode, lockUntilTime,
          ignoreUntilTime, isSystem, whitelist
      )
      Timber.i("DB: INSERT")
      val deleteResult = deleteWithPackageActivityNameUnguarded(packageName, activityName)
      Timber.d("Delete result: %d", deleteResult)
      return@fromCallable insertManager.insert(entry)
    }
        .toCompletable()
  }

  override fun updateIgnoreTime(
      packageName: String,
      activityName: String,
      ignoreUntilTime: Long
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: UPDATE IGNORE")
      return@fromCallable updateManager.updateIgnoreTime(
          packageName, activityName,
          ignoreUntilTime
      )
    }
  }

  override fun updateLockTime(
      packageName: String,
      activityName: String,
      lockUntilTime: Long
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: UPDATE LOCK")
      return@fromCallable updateManager.updateLockTime(
          packageName, activityName,
          lockUntilTime
      )
    }
  }

  override fun updateWhitelist(
      packageName: String,
      activityName: String,
      whitelist: Boolean
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: UPDATE WHITELIST")
      return@fromCallable updateManager.updateWhitelist(packageName, activityName, whitelist)
    }
  }

  @CheckResult
  override fun queryWithPackageActivityNameDefault(
      packageName: String,
      activityName: String
  ): Single<PadLockEntryModel> {
    Timber.i("DB: QUERY PACKAGE ACTIVITY DEFAULT")
    return queryManager.queryWithPackageActivityNameDefault(packageName, activityName)
  }

  @CheckResult
  override fun queryWithPackageName(
      packageName: String
  ): Single<List<PadLockEntryModel.WithPackageNameModel>> {
    Timber.i("DB: QUERY PACKAGE")
    return queryManager.queryWithPackageName(packageName)
  }

  @CheckResult
  override fun queryAll(): Single<List<PadLockEntryModel.AllEntriesModel>> {
    Timber.i("DB: QUERY ALL")
    return queryManager.queryAll()
  }

  @CheckResult
  override fun deleteWithPackageName(packageName: String): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE")
      return@fromCallable deleteManager.deleteWithPackage(packageName)
    }
  }

  @CheckResult
  override fun deleteWithPackageActivityName(
      packageName: String,
      activityName: String
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE ACTIVITY")
      return@fromCallable deleteWithPackageActivityNameUnguarded(packageName, activityName)
    }
  }

  @CheckResult
  override fun deleteAll(): Completable {
    return Completable.fromAction {
      Timber.i("DB: DELETE ALL")
      deleteManager.deleteAll()
    }
  }

  private class PadLockOpenHelper internal constructor(context: Context) : SQLiteOpenHelper(
      context.applicationContext,
      DB_NAME, null,
      DATABASE_VERSION
  ) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("onCreate")
      Timber.d("EXEC SQL: %s", PadLockEntryModel.CREATE_TABLE)
      sqLiteDatabase.execSQL(PadLockEntryModel.CREATE_TABLE)
    }

    override fun onUpgrade(
        sqLiteDatabase: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
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
      val alterWithWhitelist =
          "ALTER TABLE ${PadLockEntryModel.TABLE_NAME} ADD COLUMN ${PadLockEntryModel.WHITELIST} INTEGER NOT NULL DEFAULT 0"
      Timber.d("EXEC SQL: %s", alterWithWhitelist)
      sqLiteDatabase.execSQL(alterWithWhitelist)
    }

    private fun upgradeVersion2To3(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 drops the whole table")

      val dropOldTable = "DROP TABLE ${PadLockEntryModel.TABLE_NAME}"
      Timber.d("EXEC SQL: %s", dropOldTable)
      sqLiteDatabase.execSQL(dropOldTable)

      // Creating the table again
      onCreate(sqLiteDatabase)
    }

    private fun upgradeVersion1To2(sqLiteDatabase: SQLiteDatabase) {
      Timber.d("Upgrading from Version 1 to 2 drops the displayName column")

      // Remove the columns we don't want anymore from the table's list of columns
      Timber.d("Gather a list of the remaining columns")
      val columnsSeparated = TextUtils.join(
          ",",
          UPGRADE_1_TO_2_TABLE_COLUMNS
      )
      Timber.d("Column separated: %s", columnsSeparated)

      val tableName = PadLockEntryModel.TABLE_NAME
      val oldTable = tableName + "_old"
      val alterTable = "ALTER TABLE $tableName RENAME TO $oldTable"
      val insertIntoNewTable =
          "INSERT INTO $tableName($columnsSeparated) SELECT $columnsSeparated FROM $oldTable"
      val dropOldTable = "DROP TABLE $oldTable"

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

      private const val DB_NAME = "padlock_db"
      private const val DATABASE_VERSION = 4

      private val UPGRADE_1_TO_2_TABLE_COLUMNS = arrayOf(
          PadLockEntryModel.PACKAGENAME,
          PadLockEntryModel.ACTIVITYNAME, PadLockEntryModel.LOCKCODE,
          PadLockEntryModel.LOCKUNTILTIME,
          PadLockEntryModel.IGNOREUNTILTIME, PadLockEntryModel.SYSTEMAPPLICATION
      )
    }
  }
}
