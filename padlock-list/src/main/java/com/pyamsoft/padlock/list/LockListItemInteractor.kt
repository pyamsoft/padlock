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

import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper
import com.pyamsoft.padlock.model.LockState
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class LockListItemInteractor @Inject internal constructor(padLockDB: PadLockDB,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val cacheInteractor: LockListCacheInteractor) : LockStateModifyInteractor(padLockDB) {

  override fun modifySingleDatabaseEntry(oldLockState: LockState,
      newLockState: LockState, packageName: String, activityName: String,
      code: String?, system: Boolean): Single<LockState> {
    return super.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
        code, system).flatMap { lockState ->
      return@flatMap packageManagerWrapper.loadPackageLabel(packageName)
          .doOnSuccess {
            updateCacheEntry(it, packageName, lockState === LockState.LOCKED)
          }.flatMapSingle { Single.just(lockState) }
    }
  }

  override fun clearCache() {
    cacheInteractor.clearCache()
  }

  private fun updateCacheEntry(name: String, packageName: String, newLockState: Boolean) {
    val cached = cacheInteractor.retrieve()
    if (cached != null) {
      cacheInteractor.cache(cached.map {
        val size = it.size
        for (i in 0..size - 1) {
          val appEntry = it[i]
          if (appEntry.name() == name && appEntry.packageName() == packageName) {
            Timber.d("Update cached entry: %s %s", name, packageName)
            it[i] = appEntry.toBuilder().locked(newLockState).build()
          }
        }
        return@map it
      })
    }
  }
}
