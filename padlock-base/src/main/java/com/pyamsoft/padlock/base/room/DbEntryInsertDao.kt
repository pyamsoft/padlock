package com.pyamsoft.padlock.base.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.padlock.api.EntryInsertDao
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber

@Dao
internal abstract class DbEntryInsertDao internal constructor() : EntryInsertDao {

  init {
    Timber.i("Create new DbEntryInsertDao")
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
    return insert(object : PadLockEntryModel {

      override fun packageName(): String {
        return packageName
      }

      override fun activityName(): String {
        return activityName
      }

      override fun lockCode(): String? {
        return lockCode
      }

      override fun whitelist(): Boolean {
        return whitelist
      }

      override fun systemApplication(): Boolean {
        return isSystem
      }

      override fun ignoreUntilTime(): Long {
        return ignoreUntilTime
      }

      override fun lockUntilTime(): Long {
        return lockUntilTime
      }

    })
  }

  override fun insert(entry: PadLockEntryModel): Completable {
    return Single.just(entry)
        .map { PadLockEntryDb.fromPadLockEntryModel(it) }
        .flatMapCompletable { Completable.fromAction { daoInsert(it) } }
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  internal abstract fun daoInsert(entry: PadLockEntryDb)
}
