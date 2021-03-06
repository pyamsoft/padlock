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

package com.pyamsoft.padlock.list

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import com.pyamsoft.padlock.api.database.EntryInsertDao
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal class LockStateModifyInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val insertDao: EntryInsertDao,
  private val deleteDao: EntryDeleteDao
) : LockStateModifyInteractor {

  @CheckResult
  private fun createNewEntry(
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean,
    whitelist: Boolean
  ): Completable {
    enforcer.assertNotOnMainThread()
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName)
    return insertDao.insert(packageName, activityName, code, 0, 0, system, whitelist)
  }

  @CheckResult
  private fun deleteEntry(
    packageName: String,
    activityName: String
  ): Completable {
    enforcer.assertNotOnMainThread()
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName)
    return deleteDao.deleteWithPackageActivityName(packageName, activityName)
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
      enforcer.assertNotOnMainThread()
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
      enforcer.assertNotOnMainThread()
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
    enforcer.assertNotOnMainThread()
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
  ): Completable = Completable.defer {
    enforcer.assertNotOnMainThread()
    return@defer when {
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
}
