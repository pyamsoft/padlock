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

package com.pyamsoft.padlock.list

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDB
import com.pyamsoft.padlock.model.LockState
import io.reactivex.Single
import timber.log.Timber

abstract class LockCommonInteractor protected constructor(
    protected @JvmField val padLockDB: PadLockDB) {

  @CheckResult protected fun createNewEntry(
      packageName: String, activityName: String, code: String?,
      system: Boolean, whitelist: Boolean): Single<LockState> {
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName)
    return padLockDB.insert(packageName, activityName, code, 0, 0, system, whitelist)
        .toSingleDefault(if (whitelist) LockState.WHITELISTED else LockState.LOCKED)
  }

  @CheckResult protected fun deleteEntry(
      packageName: String, activityName: String): Single<LockState> {
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName)
    return padLockDB.deleteWithPackageActivityName(packageName, activityName)
        .toSingleDefault(LockState.DEFAULT)
  }

  @CheckResult
  open internal fun modifySingleDatabaseEntry(oldLockState: LockState,
      newLockState: LockState, packageName: String, activityName: String,
      code: String?, system: Boolean): Single<LockState> {
    if (newLockState === LockState.WHITELISTED) {
      return Single.defer<LockState> {
        val newState: Single<LockState>
        if (oldLockState === LockState.DEFAULT) {
          Timber.d("Add new as whitelisted")
          newState = createNewEntry(packageName, activityName, code, system, true)
        } else {
          // Update existing entry
          newState = Single.just(LockState.NONE)
        }
        return@defer newState
      }
    } else if (newLockState === LockState.LOCKED) {
      return Single.defer<LockState> {
        val newState: Single<LockState>
        if (oldLockState === LockState.DEFAULT) {
          Timber.d("Add new as force locked")
          newState = createNewEntry(packageName, activityName, code, system, false)
        } else {
          // Update existing entry
          newState = Single.just(LockState.NONE)
        }
        return@defer newState
      }
    } else {
      return Single.defer<LockState> {
        val newState: Single<LockState>
        if (oldLockState === LockState.DEFAULT) {
          Timber.d("Add new entry")
          newState = createNewEntry(packageName, activityName, code, system, false)
        } else {
          Timber.d("Delete existing entry")
          newState = deleteEntry(packageName, activityName)
        }
        return@defer newState
      }
    }
  }

  abstract fun clearCache()
}
