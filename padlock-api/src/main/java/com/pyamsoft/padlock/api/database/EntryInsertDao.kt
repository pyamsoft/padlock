package com.pyamsoft.padlock.api.database

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import io.reactivex.Completable

interface EntryInsertDao {

  @CheckResult
  fun insert(entry: PadLockEntryModel): Completable

  @CheckResult
  fun insert(
    packageName: String,
    activityName: String,
    lockCode: String?,
    lockUntilTime: Long,
    ignoreUntilTime: Long,
    isSystem: Boolean,
    whitelist: Boolean
  ): Completable

}
