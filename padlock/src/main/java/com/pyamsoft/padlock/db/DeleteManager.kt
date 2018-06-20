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

internal class DeleteManager internal constructor(
//  private val briteDatabase: BriteDatabase
) {

//  private val deleteWithPackage =
//    PadLockEntryModel.DeleteWithPackageName(briteDatabase.writableDatabase)
//  private val deleteWithPackageActivity =
//    PadLockEntryModel.DeleteWithPackageActivityName(briteDatabase.writableDatabase)
//  private val deleteAll = PadLockEntryModel.DeleteAll(briteDatabase.writableDatabase)

  @CheckResult
  internal fun deleteWithPackage(packageName: String): Int {
    TODO()
//    return deleteWithPackage.run {
    //    clearBindings()
//    bind(packageName)
//    return@run briteDatabase.executeUpdateDelete(table, this)
//    }
  }

  @CheckResult
  internal fun deleteWithPackageActivity(
    packageName: String,
    activityName: String
  ): Int {
    TODO()
//    return deleteWithPackageActivity.run {
//    clearBindings()
//    bind(packageName, activityName)
//    return@run briteDatabase.executeUpdateDelete(table, this)
//    }
  }

  @CheckResult
  internal fun deleteAll(): Int {
    TODO()
//    return deleteAll.run { briteDatabase.executeUpdateDelete(table, this) }
  }
}
