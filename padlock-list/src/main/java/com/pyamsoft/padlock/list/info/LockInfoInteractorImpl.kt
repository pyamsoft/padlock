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

import com.popinnow.android.repo.ObservableRepo
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@JvmSuppressWildcards
@Singleton
internal class LockInfoInteractorImpl @Inject internal constructor(
  @Named("interactor_lock_info") private val db: LockInfoInteractor,
  @Named("repo_lock_info") private val repoLockInfo: ObservableRepo<List<ActivityEntry>>,
  @Named("cache_lock_list") private val lockListCache: Cache
) : LockInfoInteractor, Cache {

  override fun modifyEntry(
    oldLockState: LockState,
    newLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable {
    return db.modifyEntry(
        oldLockState, newLockState, packageName, activityName,
        code, system
    )
        .doAfterTerminate { repoLockInfo.invalidate(packageName) }
        .doAfterTerminate { lockListCache.clearCache() }
  }

  override fun clearCache() {
    repoLockInfo.clearAll()
    lockListCache.clearCache()
  }

  override fun hasShownOnBoarding(): Single<Boolean> = db.hasShownOnBoarding()

  override fun fetchActivityEntryList(
    bypass: Boolean,
    packageName: String
  ): Observable<List<ActivityEntry>> {
    return repoLockInfo.get(bypass, packageName) { key ->
      return@get db.fetchActivityEntryList(true, key)
          // Each time the db refreshes the entire list, clear out the cache to store the new list
          .doOnNext { repoLockInfo.invalidate(key) }
    }
        .doAfterNext {
          // Cache should only ever hold the most recent list - be memory efficient
          repoLockInfo.memoryCache()
              .trimToSize(1)
        }
        .doOnError { repoLockInfo.invalidate(packageName) }
  }

  override fun calculateListDiff(
    packageName: String,
    oldList: List<ActivityEntry>,
    newList: List<ActivityEntry>
  ): Single<ListDiffResult<ActivityEntry>> =
    db.calculateListDiff(packageName, oldList, newList)
        .doOnError { repoLockInfo.invalidate(packageName) }

}
