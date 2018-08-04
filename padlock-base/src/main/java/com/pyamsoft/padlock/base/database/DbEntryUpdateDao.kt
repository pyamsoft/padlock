package com.pyamsoft.padlock.base.database

import androidx.room.Dao
import androidx.room.Query
import com.pyamsoft.padlock.api.database.EntryUpdateDao
import io.reactivex.Completable
import timber.log.Timber

@Dao
internal abstract class DbEntryUpdateDao internal constructor() : EntryUpdateDao {

  init {
    Timber.i("Create new DbEntryUpdateDao")
  }

  override fun updateIgnoreUntilTime(
    packageName: String,
    activityName: String,
    ignoreUntilTime: Long
  ): Completable {
    return Completable.fromAction {
      daoUpdateIgnoreUntilTime(packageName, activityName, ignoreUntilTime)
    }
  }

  @Query(
      "UPDATE ${PadLockEntryDb.TABLE_NAME} SET ${PadLockEntryDb.COLUMN_IGNORE_UNTIL_TIME} = :ignoreUntilTime WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName AND ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :activityName"
  )
  internal abstract fun daoUpdateIgnoreUntilTime(
    packageName: String,
    activityName: String,
    ignoreUntilTime: Long
  )

  override fun updateLockUntilTime(
    packageName: String,
    activityName: String,
    lockUntilTime: Long
  ): Completable {
    return Completable.fromAction {
      daoUpdateLockUntilTime(packageName, activityName, lockUntilTime)
    }
  }

  @Query(
      "UPDATE ${PadLockEntryDb.TABLE_NAME} SET ${PadLockEntryDb.COLUMN_LOCK_UNTIL_TIME} = :lockUntilTime WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName AND ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :activityName"
  )
  internal abstract fun daoUpdateLockUntilTime(
    packageName: String,
    activityName: String,
    lockUntilTime: Long
  )

  override fun updateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Completable {
    return Completable.fromAction {
      daoUpdateWhitelist(packageName, activityName, whitelist)
    }
  }

  @Query(
      "UPDATE ${PadLockEntryDb.TABLE_NAME} SET ${PadLockEntryDb.COLUMN_WHITELIST} = :whitelist WHERE ${PadLockEntryDb.COLUMN_PACKAGE_NAME} = :packageName AND ${PadLockEntryDb.COLUMN_ACTIVITY_NAME} = :activityName"
  )
  internal abstract fun daoUpdateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  )
}
