/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.base.database

import android.content.Context
import androidx.annotation.CheckResult
import androidx.core.database.getStringOrNull
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import com.pyamsoft.padlock.api.database.EntryInsertDao
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.database.EntryUpdateDao
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.DELETED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.INSERTED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.UPDATED
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.AsyncSubject
import timber.log.Timber

@Database(entities = [PadLockEntryDb::class], version = 1, exportSchema = true)
internal abstract class PadLockDbImpl internal constructor() : RoomDatabase(), PadLockDb {

  private val sqlDelightMigrationBus = AsyncSubject.create<Boolean>()
      .toSerialized()
  private val realtimeChangeBus = RxBus.create<EntityChangeEvent>()
  private val lock = Any()

  private val roomQueryDao = object : EntryQueryDao {

    override fun queryAll(): Single<List<AllEntriesModel>> {
      return waitForSqldelightMigration().flatMap {
        synchronized(lock) {
          return@flatMap queryDao().queryAll()
        }
      }
    }

    override fun subscribeToUpdates(): Observable<EntityChangeEvent> {
      return waitForSqldelightMigration().flatMapObservable { realtimeChangeBus.listen() }
    }

    override fun queryWithPackageName(packageName: String): Single<List<WithPackageNameModel>> {
      return waitForSqldelightMigration().flatMap {
        synchronized(lock) {
          return@flatMap queryDao().queryWithPackageName(packageName)
        }
      }
    }

    override fun queryWithPackageActivityName(
      packageName: String,
      activityName: String
    ): Single<PadLockEntryModel> {
      return waitForSqldelightMigration().flatMap {
        synchronized(lock) {
          return@flatMap queryDao().queryWithPackageActivityName(packageName, activityName)
        }
      }
    }

  }

  private val roomInsertDao = object : EntryInsertDao {

    override fun insert(entry: PadLockEntryModel): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable insertDao().insert(entry)
              .doOnComplete {
                realtimeChangeBus.publish(
                    EntityChangeEvent(
                        INSERTED,
                        entry.packageName(),
                        entry.activityName(),
                        entry.whitelist()
                    )
                )
              }
        }
      }
    }

    override fun insert(
      packageName: String,
      activityName: String,
      lockCode: String?,
      lockUntilTime: Long,
      ignoreUntilTime: Long,
      isSystem: Boolean,
      whitelist: Boolean
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable insertDao().insert(
              packageName, activityName, lockCode,
              lockUntilTime, ignoreUntilTime, isSystem, whitelist
          )
              .doOnComplete {
                realtimeChangeBus.publish(
                    EntityChangeEvent(INSERTED, packageName, activityName, whitelist)
                )
              }
        }
      }
    }

  }
  private val roomUpdateDao = object : EntryUpdateDao {
    override fun updateLockUntilTime(
      packageName: String,
      activityName: String,
      lockUntilTime: Long
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable updateDao().updateLockUntilTime(
              packageName, activityName, lockUntilTime
          )
        }
      }
    }

    override fun updateIgnoreUntilTime(
      packageName: String,
      activityName: String,
      ignoreUntilTime: Long
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable updateDao().updateIgnoreUntilTime(
              packageName, activityName, ignoreUntilTime
          )
        }
      }
    }

    override fun updateWhitelist(
      packageName: String,
      activityName: String,
      whitelist: Boolean
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable updateDao().updateWhitelist(
              packageName, activityName, whitelist
          )
              .doOnComplete {
                realtimeChangeBus.publish(
                    EntityChangeEvent(UPDATED, packageName, activityName, whitelist)
                )
              }
        }
      }
    }

  }
  private val roomDeleteDao = object : EntryDeleteDao {

    override fun deleteWithPackageName(packageName: String): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable deleteDao().deleteWithPackageName(packageName)
              .doOnComplete {
                realtimeChangeBus.publish(
                    EntityChangeEvent(DELETED, packageName, null, false)
                )
              }
        }
      }
    }

    override fun deleteWithPackageActivityName(
      packageName: String,
      activityName: String
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable deleteDao().deleteWithPackageActivityName(
              packageName, activityName
          )
              .doOnComplete {
                realtimeChangeBus.publish(
                    EntityChangeEvent(DELETED, packageName, activityName, false)
                )
              }
        }
      }
    }

    override fun deleteAll(): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        synchronized(lock) {
          return@flatMapCompletable deleteDao().deleteAll()
              .doOnComplete {
                realtimeChangeBus.publish(
                    EntityChangeEvent(DELETED, null, null, false)
                )
              }
        }
      }
    }

  }

  init {
    Timber.i("Create new PadLockDbImpl")
  }

  @CheckResult
  private fun waitForSqldelightMigration(): Single<Boolean> {
    return sqlDelightMigrationBus
        .filter { it }
        .firstOrError()
  }

  private fun sqlDelightMigrationComplete(context: Context) {
    sqlDelightMigrationBus.also {
      if (!it.hasComplete()) {
        if (oldSqlDelightExists(context)) {
          deleteOldSqlDelight(context)
        }

        it.onNext(true)
        it.onComplete()
      }
    }
  }

  @CheckResult
  private fun oldSqlDelightExists(context: Context): Boolean {
    return context.getDatabasePath(SqlDelightMigrations.SQLDELIGHT_DB_NAME)
        .exists()
  }

  private fun deleteOldSqlDelight(context: Context) {
    val name = SqlDelightMigrations.SQLDELIGHT_DB_NAME
    Timber.w("Deleting old database held at $name")
    context.deleteDatabase(name)
  }

  override fun migrateFromSqlDelight(context: Context) {
    Completable.defer {
      synchronized(lock) {
        // If the old sqldelight file exists, migrate it
        if (!oldSqlDelightExists(context)) {
          Timber.d("No old SqlDelight file exists - assume migration complete or not needed")
          sqlDelightMigrationComplete(context)
          return@defer Completable.complete()
        }

        val sqldelight = FrameworkSQLiteOpenHelperFactory().create(
            Configuration.builder(context)
                .callback(EmptyCallback)
                .name(SqlDelightMigrations.SQLDELIGHT_DB_NAME)
                .build()
        )

        // Get all entries in the DB
        val inserts = ArrayList<Completable>()
        sqldelight.readableDatabase.use { db ->
          db.query("SELECT * FROM ${SqlDelightMigrations.TABLE_NAME}")
              .use {

                // Migrate them into Room via insert
                if (it.moveToFirst()) {
                  val columnPackageName =
                    it.getColumnIndexOrThrow(SqlDelightMigrations.PACKAGE_NAME)
                  val columnActivityName =
                    it.getColumnIndexOrThrow(SqlDelightMigrations.ACTIVITY_NAME)
                  val columnLockCode = it.getColumnIndexOrThrow(SqlDelightMigrations.LOCK_CODE)
                  val columnLockUntilTime =
                    it.getColumnIndexOrThrow(SqlDelightMigrations.LOCK_UNTIL_TIME)
                  val columnIgnoreUntilTime =
                    it.getColumnIndexOrThrow(SqlDelightMigrations.IGNORE_UNTIL_TIME)
                  val columnSystemApplication =
                    it.getColumnIndexOrThrow(SqlDelightMigrations.SYSTEM_APPLICATION)

                  // Some old versions did not have whitelist column
                  val columnWhitelist = it.getColumnIndex(SqlDelightMigrations.WHITELIST)

                  while (it.moveToNext()) {
                    val packageName: String = it.getString(columnPackageName)
                    val activityName: String = it.getString(columnActivityName)
                    val lockCode: String? = it.getStringOrNull(columnLockCode)
                    val lockUntilTime: Long = it.getLong(columnLockUntilTime)
                    val ignoreUntilTime: Long = it.getLong(columnIgnoreUntilTime)
                    val systemApplication: Boolean = it.getInt(columnSystemApplication) != 0

                    // Some old versions did not have whitelist column - default to false
                    val whitelist: Boolean
                    if (columnWhitelist < 0) {
                      whitelist = false
                    } else {
                      whitelist = it.getInt(columnWhitelist) != 0
                    }

                    // Queue up the insert into the new DB, migrating old data
                    Timber.w(
                        """Migrating old:
            |(
            | packageName       = $packageName
            | activityName      = $activityName
            | lockCode          = $lockCode
            | lockUntilTime     = $lockUntilTime
            | ignoreUntilTime   = $ignoreUntilTime
            | systemApplication = $systemApplication
            | whitelist         = $whitelist
            |)""".trimMargin()
                    )
                    inserts.add(
                        insertDao().insert(
                            packageName, activityName, lockCode, lockUntilTime,
                            ignoreUntilTime, systemApplication, whitelist
                        )
                    )
                  }
                }
              }
        }

        // Wait for all the inserts to happen
        if (inserts.isEmpty()) {
          return@defer Completable.complete()
        } else {
          return@defer Completable.mergeArray(*inserts.toTypedArray())
        }
      }
    }
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(object : CompletableObserver {

          override fun onError(e: Throwable) {
            Timber.e(e, "SqlDelight migration failed, just drop the old table")
            sqlDelightMigrationComplete(context)
          }

          override fun onComplete() {
            Timber.d("Migrated old SqlDelight to Room")
            sqlDelightMigrationComplete(context)
          }

          override fun onSubscribe(d: Disposable) {
          }

        })
  }

  override fun query(): EntryQueryDao {
    return roomQueryDao
  }

  override fun insert(): EntryInsertDao {
    return roomInsertDao
  }

  override fun update(): EntryUpdateDao {
    return roomUpdateDao
  }

  override fun delete(): EntryDeleteDao {
    return roomDeleteDao
  }

  @CheckResult
  internal abstract fun queryDao(): DbEntryQueryDao

  @CheckResult
  internal abstract fun insertDao(): DbEntryInsertDao

  @CheckResult
  internal abstract fun updateDao(): DbEntryUpdateDao

  @CheckResult
  internal abstract fun deleteDao(): DbEntryDeleteDao


  private object EmptyCallback : SupportSQLiteOpenHelper.Callback(
      SqlDelightMigrations.SQLDELIGHT_DB_VERSION
  ) {

    override fun onCreate(db: SupportSQLiteDatabase?) {
      Timber.d("Callback Create is intentionally empty, this database should already be set up")
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase?,
      oldVersion: Int,
      newVersion: Int
    ) {
      Timber.d("Callback Migration is intentionally empty, this database should already be set up")
    }

  }

  companion object {

    private object SqlDelightMigrations {

      internal const val SQLDELIGHT_DB_VERSION = 4
      internal const val SQLDELIGHT_DB_NAME = "padlock_db"
      internal const val TABLE_NAME = "padlock_entry"
      internal const val PACKAGE_NAME = "packageName"
      internal const val ACTIVITY_NAME = "activityName"
      internal const val LOCK_CODE = "lockCode"
      internal const val LOCK_UNTIL_TIME = "lockUntilTime"
      internal const val IGNORE_UNTIL_TIME = "ignoreUntilTime"
      internal const val SYSTEM_APPLICATION = "systemApplication"
      internal const val WHITELIST = "whitelist"
    }

  }

}

