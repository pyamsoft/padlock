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

package com.pyamsoft.padlock.list.info

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.list.LockStateModifyInteractor
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class LockInfoItemInteractorImpl @Inject internal constructor(
    @param:Named("cache_lock_info") private val cache: Cache,
    private val updateDb: PadLockDBUpdate,
    private val modifyInteractor: LockStateModifyInteractor) : LockInfoItemInteractor {

  override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
      packageName: String, activityName: String, code: String?,
      system: Boolean): Single<LockState> {
    return modifyInteractor.modifySingleDatabaseEntry(oldLockState, newLockState, packageName,
        activityName, code, system)
        .flatMap {
          val resultState: Single<LockState>
          if (it === LockState.NONE) {
            Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated")
            resultState = updateExistingEntry(packageName, activityName,
                newLockState === LockState.WHITELISTED)
          } else {
            Timber.d("Entry handled, just pass through")
            resultState = Single.just(it)
          }

          return@flatMap resultState.doOnSuccess {
            Timber.d("Clear lock info cache")
            cache.clearCache()
          }
        }
  }

  @CheckResult private fun updateExistingEntry(
      packageName: String, activityName: String, whitelist: Boolean): Single<LockState> {
    Timber.d("Entry already exists for: %s %s, update it", packageName, activityName)
    return updateDb.updateWhitelist(whitelist, packageName, activityName)
        .toSingleDefault(if (whitelist) LockState.WHITELISTED else LockState.LOCKED)
  }
}
