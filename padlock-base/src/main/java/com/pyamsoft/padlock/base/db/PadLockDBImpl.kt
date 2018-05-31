/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.base.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.content.Context
import android.support.annotation.CheckResult
import android.text.TextUtils
import com.pyamsoft.padlock.api.PadLockDBDelete
import com.pyamsoft.padlock.api.PadLockDBInsert
import com.pyamsoft.padlock.api.PadLockDBQuery
import com.pyamsoft.padlock.api.PadLockDBUpdate
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.squareup.sqlbrite3.SqlBrite
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PadLockDBImpl @Inject internal constructor(context: Context) : PadLockDBInsert,
    PadLockDBUpdate, PadLockDBQuery, PadLockDBDelete {

  private val queryManager: QueryManager
  private val createManager: CreateManager
  private val insertManager: InsertManager
  private val deleteManager: DeleteManager
  private val updateManager: UpdateManager

  init {
    val sqlBrite: SqlBrite = SqlBrite.Builder()
        .logger {
          Timber.tag("PadLockDB")
              .d(it)
        }
        .build()
    val dbConfiguration: SupportSQLiteOpenHelper.Configuration =
      SupportSQLiteOpenHelper.Configuration.builder(context)
          .callback(PadLockOpenHelper())
          .name(DB_NAME)
          .build()
    val briteDatabase = sqlBrite.wrapDatabaseHelper(
        FrameworkSQLiteOpenHelperFactory().create(dbConfiguration), Schedulers.computation()
    )

    val entryFactory: PadLockEntryModel.Factory<*> = PadLockSqlEntry.createFactory()
    queryManager = QueryManager(briteDatabase, entryFactory)
    createManager = CreateManager(entryFactory)
    insertManager = InsertManager(briteDatabase)
    deleteManager = DeleteManager(briteDatabase)
    updateManager = UpdateManager(briteDatabase)
  }

  @CheckResult
  private fun deleteWithPackageActivityNameUnguarded(
    packageName: String,
    activityName: String
  ): Int = deleteManager.deleteWithPackageActivity(packageName, activityName)

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
          packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime, isSystem, whitelist
      )
      Timber.i("DB: INSERT $packageName $activityName")
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
      Timber.i("DB: UPDATE IGNORE $packageName $activityName")
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
      Timber.i("DB: UPDATE LOCK $packageName $activityName")
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
      Timber.i("DB: UPDATE WHITELIST $packageName $activityName")
      return@fromCallable updateManager.updateWhitelist(packageName, activityName, whitelist)
    }
  }

  @CheckResult
  override fun queryWithPackageActivityNameDefault(
    packageName: String,
    activityName: String
  ): Single<PadLockEntryModel> {
    Timber.i("DB: QUERY PACKAGE ACTIVITY DEFAULT $packageName $activityName")
    return queryManager.queryWithPackageActivityNameDefault(packageName, activityName)
  }

  @CheckResult
  override fun queryWithPackageName(
    packageName: String
  ): Single<List<PadLockEntryModel.WithPackageNameModel>> {
    Timber.i("DB: QUERY PACKAGE $packageName")
    return queryManager.queryWithPackageName(packageName)
  }

  @CheckResult
  override fun queryAll(): Observable<List<PadLockEntryModel.AllEntriesModel>> {
    return queryManager.queryAll()
  }

  @CheckResult
  override fun deleteWithPackageName(packageName: String): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE $packageName")
      return@fromCallable deleteManager.deleteWithPackage(packageName)
    }
  }

  @CheckResult
  override fun deleteWithPackageActivityName(
    packageName: String,
    activityName: String
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE ACTIVITY $packageName $activityName")
      return@fromCallable deleteWithPackageActivityNameUnguarded(packageName, activityName)
    }
  }

  @CheckResult
  override fun deleteAll(): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE ALL")
      return@fromCallable deleteManager.deleteAll()
    }
  }

  private class PadLockOpenHelper internal constructor() : SupportSQLiteOpenHelper.Callback(
      DATABASE_VERSION
  ) {

    override fun onCreate(db: SupportSQLiteDatabase) {
      Timber.d("onCreate")
      Timber.d("EXEC SQL: %s", PadLockEntryModel.CREATE_TABLE)
      db.execSQL(PadLockEntryModel.CREATE_TABLE)
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase,
      oldVersion: Int,
      newVersion: Int
    ) {
      Timber.d("onUpgrade from old version %d to new %d", oldVersion, newVersion)
      var currentVersion = oldVersion
      if (currentVersion == 1 && newVersion >= 2) {
        upgradeVersion1To2(db)
        ++currentVersion
      }

      if (currentVersion == 2 && newVersion >= 3) {
        upgradeVersion2To3(db)
        ++currentVersion
      }

      if (currentVersion == 3 && newVersion >= 4) {
        upgradeVersion3To4(db)
        ++currentVersion
      }
    }

    private fun upgradeVersion3To4(db: SupportSQLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 adds whitelist column")
      val alterWithWhitelist =
        "ALTER TABLE $TABLE_NAME ADD COLUMN $WHITELIST INTEGER NOT NULL DEFAULT 0"
      Timber.d("EXEC SQL: %s", alterWithWhitelist)
      db.execSQL(alterWithWhitelist)
    }

    private fun upgradeVersion2To3(db: SupportSQLiteDatabase) {
      Timber.d("Upgrading from Version 2 to 3 drops the whole table")

      val dropOldTable = "DROP TABLE $TABLE_NAME"
      Timber.d("EXEC SQL: %s", dropOldTable)
      db.execSQL(dropOldTable)

      // Creating the table again
      onCreate(db)
    }

    private fun upgradeVersion1To2(db: SupportSQLiteDatabase) {
      Timber.d("Upgrading from Version 1 to 2 drops the displayName column")

      // Remove the columns we don't want anymore from the table's list of columns
      Timber.d("Gather a list of the remaining columns")
      val columnsSeparated = TextUtils.join(
          ",",
          UPGRADE_1_TO_2_TABLE_COLUMNS
      )
      Timber.d("Column separated: %s", columnsSeparated)

      val tableName = TABLE_NAME
      val oldTable = tableName + "_old"
      val alterTable = "ALTER TABLE $tableName RENAME TO $oldTable"
      val insertIntoNewTable =
        "INSERT INTO $tableName($columnsSeparated) SELECT $columnsSeparated FROM $oldTable"
      val dropOldTable = "DROP TABLE $oldTable"

      // Move the existing table to an old table
      Timber.d("EXEC SQL: %s", alterTable)
      db.execSQL(alterTable)

      onCreate(db)

      // Populating the table with the data
      Timber.d("EXEC SQL: %s", insertIntoNewTable)
      db.execSQL(insertIntoNewTable)

      Timber.d("EXEC SQL: %s", dropOldTable)
      db.execSQL(dropOldTable)
    }

    companion object {

      private const val DATABASE_VERSION = 4

      // We redefine the table bits here because during migration the names may have changed
      // Valid for DB version 4
      private const val TABLE_NAME = "padlock_entry"
      private const val PACKAGE_NAME = "packageName"
      private const val ACTIVITY_NAME = "activityName"
      private const val LOCK_CODE = "lockCode"
      private const val LOCK_UNTIL_TIME = "lockUntilTime"
      private const val IGNORE_UNTIL_TIME = "ignoreUntilTime"
      private const val SYSTEM_APPLICATION = "systemApplication"
      private const val WHITELIST = "whitelist"

      private val UPGRADE_1_TO_2_TABLE_COLUMNS = arrayOf(
          PACKAGE_NAME, ACTIVITY_NAME, LOCK_CODE, LOCK_UNTIL_TIME, IGNORE_UNTIL_TIME,
          SYSTEM_APPLICATION
      )
    }
  }

  companion object {
    private const val DB_NAME = "padlock_db"
  }
}
