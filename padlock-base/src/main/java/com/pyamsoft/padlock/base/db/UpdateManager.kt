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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.squareup.sqlbrite3.BriteDatabase

internal class UpdateManager internal constructor(briteDatabase: BriteDatabase) {

  private val updateWhitelist by lazy {
    PadLockEntryModel.UpdateWhitelist(briteDatabase.writableDatabase)
  }

  private val updateIgnoreTime by lazy {
    PadLockEntryModel.UpdateIgnoreUntilTime(briteDatabase.writableDatabase)
  }

  private val updateHardLocked by lazy {
    PadLockEntryModel.UpdateLockUntilTime(briteDatabase.writableDatabase)
  }

  @CheckResult
  internal fun updateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Int {
    if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
      throw RuntimeException("Cannot update whitelist EMPTY entry")
    }

    return updateWhitelist.run {
      clearBindings()
      bind(whitelist, packageName, activityName)
      executeUpdateDelete()
    }
  }

  @CheckResult
  internal fun updateIgnoreTime(
    packageName: String,
    activityName: String,
    ignoreTime: Long
  ): Int {
    if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
      throw RuntimeException("Cannot update ignore time EMPTY entry")
    }

    return updateIgnoreTime.run {
      clearBindings()
      bind(ignoreTime, packageName, activityName)
      executeUpdateDelete()
    }
  }

  @CheckResult
  internal fun updateLockTime(
    packageName: String,
    activityName: String,
    lockTime: Long
  ): Int {
    if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
      throw RuntimeException("Cannot update lock time EMPTY entry")
    }

    return updateHardLocked.run {
      clearBindings()
      bind(lockTime, packageName, activityName)
      executeUpdateDelete()
    }
  }
}
