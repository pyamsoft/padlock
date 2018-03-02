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

package com.pyamsoft.padlock.list.info

import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockInfoUpdater
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.cache.TimedMap
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockInfoInteractorCache @Inject internal constructor(
    @param:Named("interactor_lock_info") private val impl: LockInfoInteractor
) : LockInfoInteractor, Cache, LockInfoUpdater {

  private val infoCache = TimedMap<String, Single<MutableList<ActivityEntry>>>()

  override fun modifySingleDatabaseEntry(
      oldLockState: LockState,
      newLockState: LockState,
      packageName: String,
      activityName: String,
      code: String?,
      system: Boolean
  ): Single<LockState> {
    return impl.modifySingleDatabaseEntry(
        oldLockState, newLockState, packageName, activityName,
        code, system
    )
        .doOnSuccess {
          infoCache.updateIfAvailable(packageName) { single ->
            single.doOnSuccess {
              for ((index, entry) in it.withIndex()) {
                if (entry.packageName == packageName && entry.name == activityName) {
                  it[index] = ActivityEntry(
                      name = entry.name,
                      packageName = entry.packageName,
                      lockState = newLockState
                  )
                  break
                }
              }
            }
          }
        }
        .doOnError { infoCache.remove(packageName) }
  }

  override fun update(
      packageName: String,
      activityName: String,
      lockState: LockState
  ): Completable {
    return Completable.fromAction {
      infoCache.updateIfAvailable(packageName) { single ->
        single.doOnSuccess {
          for ((index, entry) in it.withIndex()) {
            if (entry.packageName == packageName && entry.name == activityName) {
              it[index] = ActivityEntry(
                  name = entry.name, packageName = entry.packageName,
                  lockState = lockState
              )
              break
            }
          }
        }
      }
    }
        .doOnError { infoCache.remove(packageName) }
  }

  override fun clearCache() {
    infoCache.clearCache()
  }

  override fun hasShownOnBoarding(): Single<Boolean> = impl.hasShownOnBoarding()

  override fun fetchActivityEntryList(
      force: Boolean,
      packageName: String
  ): Single<List<ActivityEntry>> {
    return infoCache.getElseFresh(force, packageName) {
      impl.fetchActivityEntryList(true, packageName)
          .map { it.toMutableList() }
          .cache()
    }
        .map { it.toList() }
        .doOnError { infoCache.remove(packageName) }
  }

  override fun calculateListDiff(
      packageName: String,
      oldList: List<ActivityEntry>,
      newList: List<ActivityEntry>
  ): Single<ListDiffResult<ActivityEntry>> =
      impl.calculateListDiff(packageName, oldList, newList)
          .doOnError { infoCache.remove(packageName) }

}
