package com.pyamsoft.padlock.base.database

import androidx.room.Dao
import androidx.room.Query
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import io.reactivex.Completable
import timber.log.Timber

@Dao
internal abstract class DbEntryDeleteDao internal constructor() : EntryDeleteDao {

  init {
    Timber.i("Create new DbEntryDeleteDao")
  }

  override fun deleteWithPackageName(packageName: String): Completable {
    return Completable.fromAction { daoDeleteWithPackageName(packageName) }
  }

  @Query(
      "DELETE FROM ${PadLockEntryDb.TABLE_NAME} WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName"
  )
  internal abstract fun daoDeleteWithPackageName(packageName: String)

  override fun deleteWithPackageActivityName(
    packageName: String,
    activityName: String
  ): Completable {
    return Completable.fromAction { daoDeleteWithPackageActivityName(packageName, activityName) }
  }

  @Query(
      "DELETE FROM ${PadLockEntryDb.TABLE_NAME} WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName AND ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :activityName"
  )
  internal abstract fun daoDeleteWithPackageActivityName(
    packageName: String,
    activityName: String
  )

  override fun deleteAll(): Completable {
    return Completable.fromAction { daoDeleteAll() }
  }

  @Query("DELETE FROM ${PadLockEntryDb.TABLE_NAME} WHERE 1 = 1")
  internal abstract fun daoDeleteAll()

}
