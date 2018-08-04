package com.pyamsoft.padlock.base.database

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import com.pyamsoft.padlock.api.EntryQueryDao
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

@Dao
internal abstract class DbEntryQueryDao internal constructor() : EntryQueryDao {

  init {
    Timber.i("Create new DbEntryQueryDao")
  }

  override fun subscribeToUpdates(): Observable<EntityChangeEvent> {
    throw RuntimeException(
        "The DbEntryQueryDao does not power this subscribeToUpdates() method. See PadLockDbImpl."
    )
  }

  override fun queryAll(): Single<List<AllEntriesModel>> {
    return daoQueryAll()
        .toSingle(emptyList())
        .map { it }
  }

  @Query(
      "SELECT ${PadLockEntryDb.COLUMN_PACKAGE_NAME}, ${PadLockEntryDb.COLUMN_ACTIVITY_NAME}, ${PadLockEntryDb.COLUMN_WHITELIST} FROM ${PadLockEntryDb.TABLE_NAME}"
  )
  // We intentionally do not ask for everything
  @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
  @CheckResult
  internal abstract fun daoQueryAll(): Maybe<List<PadLockEntryDb>>

  override fun queryWithPackageName(packageName: String): Single<List<WithPackageNameModel>> {
    return daoQueryWithPackageName(packageName)
        .toSingle(emptyList())
        .map { it }
  }

  @Query(
      "SELECT ${PadLockEntryDb.COLUMN_PACKAGE_NAME}, ${PadLockEntryDb.COLUMN_ACTIVITY_NAME}, ${PadLockEntryDb.COLUMN_WHITELIST} FROM ${PadLockEntryDb.TABLE_NAME} WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName"
  )
  // We intentionally do not ask for everything
  @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
  @CheckResult
  internal abstract fun daoQueryWithPackageName(packageName: String): Maybe<List<PadLockEntryDb>>

  override fun queryWithPackageActivityName(
    packageName: String,
    activityName: String
  ): Single<PadLockEntryModel> {
    return daoQueryWithPackageActivityName(
        packageName, activityName, PadLockDbModels.PACKAGE_ACTIVITY_NAME
    )
        .firstOrError()
        .map { it }
  }

  @Query(
      "SELECT * FROM ${PadLockEntryDb.TABLE_NAME} WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName AND (${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :defaultPackageActivityName OR ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :activityName) ORDER BY CASE WHEN ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :defaultPackageActivityName THEN 1 WHEN ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :activityName THEN 0 END ASC LIMIT 1"
  )
  @CheckResult
  internal abstract fun daoQueryWithPackageActivityName(
    packageName: String,
    activityName: String,
    defaultPackageActivityName: String
  ): Flowable<PadLockEntryDb>

}
