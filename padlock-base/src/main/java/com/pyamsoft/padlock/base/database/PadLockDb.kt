package com.pyamsoft.padlock.base.database

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import com.pyamsoft.padlock.api.database.EntryInsertDao
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.database.EntryUpdateDao

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

