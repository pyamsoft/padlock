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
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class LockInfoItemInteractor @Inject internal constructor(padLockDB: PadLockDB,
    internal val cacheInteractor: LockInfoCacheInteractor) : LockCommonInteractor(padLockDB) {

  override fun clearCache() {
    cacheInteractor.clearCache()
  }

  override fun modifySingleDatabaseEntry(oldLockState: LockState,
      newLockState: LockState, packageName: String, activityName: String,
      code: String?, system: Boolean): Single<LockState> {
    return super.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
        code, system).flatMap {
      val resultState: Single<LockState>
      if (it === LockState.NONE) {
        Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated")
        resultState = updateExistingEntry(packageName, activityName,
            newLockState === LockState.WHITELISTED)
      } else {
        resultState = Single.fromCallable {
          updateCacheEntry(packageName, activityName, it)
          return@fromCallable it
        }
      }
      return@flatMap resultState
    }
  }

  @CheckResult private fun updateExistingEntry(
      packageName: String, activityName: String, whitelist: Boolean): Single<LockState> {
    Timber.d("Entry already exists for: %s %s, update it", packageName, activityName)
    return padLockDB.updateWhitelist(whitelist, packageName, activityName)
        .toSingleDefault(if (whitelist) LockState.WHITELISTED else LockState.LOCKED)
        .doOnSuccess { updateCacheEntry(packageName, activityName, it) }
  }

  private fun updateCacheEntry(packageName: String,
      name: String, lockState: LockState) {
    val cached = cacheInteractor.getFromCache(packageName)
    if (cached != null) {
      Timber.d("Attempt update cached entry for: %s", packageName)
      cacheInteractor.putIntoCache(packageName, cached.map {
        val size = it.size
        for (i in 0..size - 1) {
          val activityEntry = it[i]
          if (activityEntry.name() == name) {
            Timber.d("Update cached entry: %s %s", name, lockState)
            it[i] = ActivityEntry.builder().name(activityEntry.name()).lockState(
                lockState).build()
          }
        }
        return@map it
      })
    }
  }
}
