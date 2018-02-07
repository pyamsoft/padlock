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

import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.squareup.sqlbrite2.BriteDatabase

internal class DeleteManager internal constructor(
    openHelper: SQLiteOpenHelper,
    private val briteDatabase: BriteDatabase
) {

  private val deleteWithPackage by lazy {
    PadLockEntryModel.DeleteWithPackageName(openHelper.writableDatabase)
  }

  private val deleteWithPackageActivity by lazy {
    PadLockEntryModel.DeleteWithPackageActivityName(openHelper.writableDatabase)
  }

  private val deleteAll by lazy {
    PadLockEntryModel.DeleteAll(openHelper.writableDatabase)
  }

  @CheckResult
  internal fun deleteWithPackage(packageName: String): Long {
    return briteDatabase.bindAndExecute(deleteWithPackage) {
      bind(packageName)
    }
  }

  @CheckResult
  internal fun deleteWithPackageActivity(
      packageName: String,
      activityName: String
  ): Long {
    return briteDatabase.bindAndExecute(deleteWithPackageActivity) {
      bind(packageName, activityName)
    }
  }

  internal fun deleteAll(): Long = briteDatabase.bindAndExecute(deleteAll) {}
}