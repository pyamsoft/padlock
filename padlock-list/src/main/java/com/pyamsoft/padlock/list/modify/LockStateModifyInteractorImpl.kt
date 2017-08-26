/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.list.modify

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.db.PadLockDBInsert
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.NONE
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

internal class LockStateModifyInteractorImpl @Inject internal constructor(
    private val insertDb: PadLockDBInsert,
    private val deleteDb: PadLockDBDelete) : LockStateModifyInteractor {

  @CheckResult private fun createNewEntry(
      packageName: String, activityName: String, code: String?,
      system: Boolean, whitelist: Boolean): Single<LockState> {
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName)
    return insertDb.insert(packageName, activityName, code, 0, 0, system, whitelist)
        .toSingleDefault(if (whitelist) LockState.WHITELISTED else LockState.LOCKED)
  }

  @CheckResult private fun deleteEntry(
      packageName: String, activityName: String): Single<LockState> {
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName)
    return deleteDb.deleteWithPackageActivityName(packageName, activityName)
        .toSingleDefault(LockState.DEFAULT)
  }

  @CheckResult private fun whitelistEntry(oldLockState: LockState, packageName: String,
      activityName: String,
      code: String?, system: Boolean): Single<LockState> {
    return Single.defer<LockState> {
      if (oldLockState === WHITELISTED) {
        // Update existing entry
        Timber.d("Update existing entry to NONE")
        return@defer Single.just(NONE)
      } else {
        Timber.d("Add new as whitelisted")
        return@defer createNewEntry(packageName, activityName, code, system, true)
      }
    }
  }

  @CheckResult private fun forceLockEntry(oldLockState: LockState, packageName: String,
      activityName: String,
      code: String?, system: Boolean): Single<LockState> {
    return Single.defer<LockState> {
      if (oldLockState === LOCKED) {
        // Update existing entry
        Timber.d("Update existing entry to NONE")
        return@defer Single.just(NONE)
      } else {
        Timber.d("Add new as force locked")
        return@defer createNewEntry(packageName, activityName, code, system, false)
      }
    }
  }

  @CheckResult private fun addNewEntry(oldLockState: LockState, packageName: String,
      activityName: String,
      code: String?, system: Boolean): Single<LockState> {
    return Single.defer<LockState> {
      if (oldLockState === LockState.DEFAULT) {
        Timber.d("Add new entry")
        return@defer createNewEntry(packageName, activityName, code, system, false)
      } else {
        Timber.d("Delete existing entry")
        return@defer deleteEntry(packageName, activityName)
      }
    }
  }

  override fun modifySingleDatabaseEntry(oldLockState: LockState,
      newLockState: LockState, packageName: String, activityName: String,
      code: String?, system: Boolean): Single<LockState> = when {
    newLockState === LockState.WHITELISTED -> whitelistEntry(oldLockState, packageName,
        activityName, code, system)
    newLockState === LockState.LOCKED -> forceLockEntry(oldLockState, packageName, activityName,
        code, system)
    else -> addNewEntry(oldLockState, packageName, activityName, code, system)
  }
}
