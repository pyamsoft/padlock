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

package com.pyamsoft.padlock.db

import androidx.annotation.CheckResult

internal class UpdateManager internal constructor(
//  private val briteDatabase: BriteDatabase
) {

//  private val updateWhitelist by lazy {
//    PadLockEntryModel.UpdateWhitelist(briteDatabase.writableDatabase)
//  }
//
//  private val updateIgnoreTime by lazy {
//    PadLockEntryModel.UpdateIgnoreUntilTime(briteDatabase.writableDatabase)
//  }
//
//  private val updateHardLocked by lazy {
//    PadLockEntryModel.UpdateLockUntilTime(briteDatabase.writableDatabase)
//  }

  @CheckResult
  internal fun updateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Int {
    TODO()
//    if (PadLockDbModels.PACKAGE_EMPTY == packageName || PadLockDbModels.ACTIVITY_EMPTY == activityName) {
//      throw RuntimeException("Cannot update whitelist EMPTY entry")
//    }
//
//    return updateWhitelist.run {
//      clearBindings()
//      bind(whitelist, packageName, activityName)
//      return@run briteDatabase.executeUpdateDelete(table, this)
//    }
  }

  @CheckResult
  internal fun updateIgnoreTime(
    packageName: String,
    activityName: String,
    ignoreTime: Long
  ): Int {
    TODO()
//    if (PadLockDbModels.PACKAGE_EMPTY == packageName || PadLockDbModels.ACTIVITY_EMPTY == activityName) {
//      throw RuntimeException("Cannot update ignore time EMPTY entry")
//    }
//
//    return updateIgnoreTime.run {
//      clearBindings()
//      bind(ignoreTime, packageName, activityName)
//      return@run briteDatabase.executeUpdateDelete(table, this)
//    }
  }

  @CheckResult
  internal fun updateLockTime(
    packageName: String,
    activityName: String,
    lockTime: Long
  ): Int {
    TODO()
//    if (PadLockDbModels.PACKAGE_EMPTY == packageName || PadLockDbModels.ACTIVITY_EMPTY == activityName) {
//      throw RuntimeException("Cannot update lock time EMPTY entry")
//    }
//
//    return updateHardLocked.run {
//      clearBindings()
//      bind(lockTime, packageName, activityName)
//      return@run briteDatabase.executeUpdateDelete(table, this)
//    }
  }
}
