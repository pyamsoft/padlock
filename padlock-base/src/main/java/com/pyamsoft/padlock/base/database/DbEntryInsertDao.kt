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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.padlock.api.database.EntryInsertDao
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
