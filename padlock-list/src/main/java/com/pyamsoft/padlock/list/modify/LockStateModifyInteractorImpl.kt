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

package com.pyamsoft.padlock.list.modify

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.PadLockDatabaseDelete
import com.pyamsoft.padlock.api.PadLockDatabaseInsert
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import io.reactivex.Completable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockStateModifyInteractorImpl @Inject internal constructor(
  private val insertDatabase: PadLockDatabaseInsert,
  private val deleteDatabase: PadLockDatabaseDelete
) : LockStateModifyInteractor {

  @CheckResult
  private fun createNewEntry(
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean,
    whitelist: Boolean
  ): Completable {
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName)
    return insertDatabase.insert(packageName, activityName, code, 0, 0, system, whitelist)
  }

  @CheckResult
  private fun deleteEntry(
    packageName: String,
    activityName: String
  ): Completable {
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName)
    return deleteDatabase.deleteWithPackageActivityName(packageName, activityName)
  }

  @CheckResult
  private fun whitelistEntry(
    oldLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable {
    return Completable.defer {
      if (oldLockState === WHITELISTED) {
        // Update existing entry
        throw RuntimeException("Can't whitelist, already whitelisted: $packageName $activityName")
      } else {
        Timber.d("Add new as whitelisted")
        return@defer createNewEntry(packageName, activityName, code, system, true)
      }
    }
  }

  @CheckResult
  private fun forceLockEntry(
    oldLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable {
    return Completable.defer {
      if (oldLockState === LOCKED) {
        // Update existing entry
        throw RuntimeException("Can't lock, already locked: $packageName $activityName")
      } else {
        Timber.d("Add new as force locked")
        return@defer createNewEntry(packageName, activityName, code, system, false)
      }
    }
  }

  @CheckResult
  private fun addNewEntry(
    oldLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable {
    return Completable.defer {
      if (oldLockState === LockState.DEFAULT) {
        return@defer createNewEntry(packageName, activityName, code, system, false)
      } else {
        return@defer deleteEntry(packageName, activityName)
      }
    }
  }

  override fun modifyEntry(
    oldLockState: LockState,
    newLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable = when {
    newLockState === LockState.WHITELISTED -> whitelistEntry(
        oldLockState, packageName,
        activityName, code, system
    )
    newLockState === LockState.LOCKED -> forceLockEntry(
        oldLockState, packageName, activityName,
        code, system
    )
    else -> addNewEntry(oldLockState, packageName, activityName, code, system)
  }
}
