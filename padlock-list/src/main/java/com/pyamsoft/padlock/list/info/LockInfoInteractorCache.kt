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
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockInfoInteractorCache @Inject internal constructor(
    @param:Named(
        "interactor_lock_info"
    ) private val impl: LockInfoInteractor
) : LockInfoInteractor,
    Cache, LockInfoUpdater {

  private var infoCache: MutableMap<String, Pair<Single<MutableList<ActivityEntry>>?, Long>> =
      HashMap()

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
          val obj: MutableMap<String, Pair<Single<MutableList<ActivityEntry>>?, Long>>? = infoCache
          if (obj != null) {
            val cached: Single<MutableList<ActivityEntry>>? = obj[packageName]?.first
            if (cached != null) {
              obj[packageName] = Pair(cached.doOnSuccess {
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
              }, System.currentTimeMillis())
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
      val obj: MutableMap<String, Pair<Single<MutableList<ActivityEntry>>?, Long>>? = infoCache
      if (obj != null) {
        val pair: Pair<Single<MutableList<ActivityEntry>>?, Long>? = obj[packageName]
        val cached: Single<MutableList<ActivityEntry>>? = pair?.first
        val time: Long = pair?.second ?: 0
        if (cached != null && time > 0) {
          obj[packageName] = Pair(cached.doOnSuccess {
            for ((index, entry) in it.withIndex()) {
              if (entry.packageName == packageName && entry.name == activityName) {
                it[index] = ActivityEntry(
                    name = entry.name, packageName = entry.packageName,
                    lockState = lockState
                )
                break
              }
            }
          }, time)
        }
      }
    }
  }

  override fun clearCache() {
    infoCache.clear()
  }

  override fun hasShownOnBoarding(): Single<Boolean> = impl.hasShownOnBoarding()

  override fun fetchActivityEntryList(
      packageName: String,
      force: Boolean
  ): Single<List<ActivityEntry>> {
    return Single.defer {
      val currentTime = System.currentTimeMillis()
      val list: Single<List<ActivityEntry>>
      val cachedPair: Pair<Single<MutableList<ActivityEntry>>?, Long>? = infoCache[packageName]
      val cache: Single<MutableList<ActivityEntry>>?
      val cachedTime: Long
      if (cachedPair == null) {
        cache = null
        cachedTime = 0L
      } else {
        cache = cachedPair.first
        cachedTime = cachedPair.second
      }
      if (force || cache == null || cachedTime + FIVE_MINUTES_MILLIS < currentTime) {
        list = impl.fetchActivityEntryList(packageName, true)
            .cache()
        infoCache[packageName] = Pair(list.map { it.toMutableList() }, currentTime)
      } else {
        list = cache.map { it.toList() }
      }
      return@defer list
    }
        .doOnError { infoCache.remove(packageName) }
  }

  override fun calculateListDiff(
      packageName: String,
      oldList: List<ActivityEntry>,
      newList: List<ActivityEntry>
  ): Single<ListDiffResult<ActivityEntry>> =
      impl.calculateListDiff(packageName, oldList, newList).doOnError {
        infoCache.remove(packageName)
      }

  companion object {

    private val FIVE_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(5L)
  }
}
