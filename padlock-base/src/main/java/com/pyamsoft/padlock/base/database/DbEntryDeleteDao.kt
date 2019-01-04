/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
