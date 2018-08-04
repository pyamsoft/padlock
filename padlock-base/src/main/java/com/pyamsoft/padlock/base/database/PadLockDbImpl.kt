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

  private val bus = RxBus.create<EntityChangeEvent>()

  private val queryDao = object : EntryQueryDao {

    override fun queryAll(): Single<List<AllEntriesModel>> {
      return waitForSqldelightMigration().flatMap { queryDao().queryAll() }
    }

    override fun subscribeToUpdates(): Observable<EntityChangeEvent> {
      return waitForSqldelightMigration().flatMapObservable { bus.listen() }
    }

    override fun queryWithPackageName(packageName: String): Single<List<WithPackageNameModel>> {
      return waitForSqldelightMigration().flatMap { queryDao().queryWithPackageName(packageName) }
    }

    override fun queryWithPackageActivityName(
      packageName: String,
      activityName: String
    ): Single<PadLockEntryModel> {
      return waitForSqldelightMigration().flatMap {
        queryDao().queryWithPackageActivityName(packageName, activityName)
      }
    }

  }

  private val insertDao = object : EntryInsertDao {

    override fun insert(entry: PadLockEntryModel): Completable {
      return waitForSqldelightMigration().flatMapCompletable { insertDao().insert(entry) }
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(
                    INSERTED,
                    entry.packageName(),
                    entry.activityName(),
                    entry.whitelist()
                )
            )
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
      return insertDao().insert(
          packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime, isSystem, whitelist
      )
          .doOnComplete {
            bus.publish(EntityChangeEvent(INSERTED, packageName, activityName, whitelist))
          }
    }

  }
  private val updateDao = object : EntryUpdateDao {
    override fun updateLockUntilTime(
      packageName: String,
      activityName: String,
      lockUntilTime: Long
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        updateDao().updateLockUntilTime(packageName, activityName, lockUntilTime)
      }
    }

    override fun updateIgnoreUntilTime(
      packageName: String,
      activityName: String,
      ignoreUntilTime: Long
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        updateDao().updateIgnoreUntilTime(packageName, activityName, ignoreUntilTime)
      }
    }

    override fun updateWhitelist(
      packageName: String,
      activityName: String,
      whitelist: Boolean
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        updateDao().updateWhitelist(packageName, activityName, whitelist)
      }
          .doOnComplete {
            bus.publish(EntityChangeEvent(UPDATED, packageName, activityName, whitelist))
          }
    }

  }
  private val deleteDao = object : EntryDeleteDao {

    override fun deleteWithPackageName(packageName: String): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        deleteDao().deleteWithPackageName(packageName)
      }
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(DELETED, packageName, null, false)
            )
          }
    }

    override fun deleteWithPackageActivityName(
      packageName: String,
      activityName: String
    ): Completable {
      return waitForSqldelightMigration().flatMapCompletable {
        deleteDao().deleteWithPackageActivityName(packageName, activityName)
      }
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(DELETED, packageName, activityName, false)
            )
          }
    }

    override fun deleteAll(): Completable {
      return waitForSqldelightMigration().flatMapCompletable { deleteDao().deleteAll() }
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(DELETED, null, null, false)
            )
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

      val inserts = ArrayList<Completable>()

      // Get all entries in the DB
      val cursor =
        sqldelight.readableDatabase.query("SELECT * FROM ${SqlDelightMigrations.TABLE_NAME}")

      // Migrate them into Room via insert
      if (cursor.moveToFirst()) {
        val columnPackageName = cursor.getColumnIndexOrThrow(SqlDelightMigrations.PACKAGE_NAME)
        val columnActivityName = cursor.getColumnIndexOrThrow(SqlDelightMigrations.ACTIVITY_NAME)
        val columnLockCode = cursor.getColumnIndexOrThrow(SqlDelightMigrations.LOCK_CODE)
        val columnLockUntilTime = cursor.getColumnIndexOrThrow(SqlDelightMigrations.LOCK_UNTIL_TIME)
        val columnIgnoreUntilTime =
          cursor.getColumnIndexOrThrow(SqlDelightMigrations.IGNORE_UNTIL_TIME)
        val columnSystemApplication =
          cursor.getColumnIndexOrThrow(SqlDelightMigrations.SYSTEM_APPLICATION)

        // Some old versions did not have whitelist column
        val columnWhitelist = cursor.getColumnIndex(SqlDelightMigrations.WHITELIST)

        while (cursor.moveToNext()) {
          val packageName: String = cursor.getString(columnPackageName)
          val activityName: String = cursor.getString(columnActivityName)
          val lockCode: String? = cursor.getStringOrNull(columnLockCode)
          val lockUntilTime: Long = cursor.getLong(columnLockUntilTime)
          val ignoreUntilTime: Long = cursor.getLong(columnIgnoreUntilTime)
          val systemApplication: Boolean = cursor.getInt(columnSystemApplication) != 0

          // Some old versions did not have whitelist column - default to false
          val whitelist: Boolean
          if (columnWhitelist < 0) {
            whitelist = false
          } else {
            whitelist = cursor.getInt(columnWhitelist) != 0
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

      // Close the database once we're done reading
      sqldelight.close()

      // Wait for all the inserts to happen
      return@defer Completable.mergeArray(*inserts.toTypedArray())
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
    return queryDao
  }

  @CheckResult
  internal abstract fun queryDao(): DbEntryQueryDao

  override fun insert(): EntryInsertDao {
    return insertDao
  }

  @CheckResult
  internal abstract fun insertDao(): DbEntryInsertDao

  override fun update(): EntryUpdateDao {
    return updateDao
  }

  @CheckResult
  internal abstract fun updateDao(): DbEntryUpdateDao

  override fun delete(): EntryDeleteDao {
    return deleteDao
  }

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

