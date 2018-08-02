package com.pyamsoft.padlock.base.room

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.EntryDeleteDao
import com.pyamsoft.padlock.api.EntryInsertDao
import com.pyamsoft.padlock.api.EntryQueryDao
import com.pyamsoft.padlock.api.EntryUpdateDao

internal interface PadLockDb {

  @CheckResult
  fun query(): EntryQueryDao

  @CheckResult
  fun insert(): EntryInsertDao

  @CheckResult
  fun update(): EntryUpdateDao

  @CheckResult
  fun delete(): EntryDeleteDao

  companion object {
    internal const val DATABASE_NAME = "PadLock-Database.db"
  }
}

