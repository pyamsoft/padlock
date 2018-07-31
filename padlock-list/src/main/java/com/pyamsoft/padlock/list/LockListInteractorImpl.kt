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

package com.pyamsoft.padlock.list

import com.popinnow.android.repo.ObservableRepo
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockListInteractorImpl @Inject internal constructor(
  @param:Named("interactor_lock_list") private val db: LockListInteractor,
  @param:Named("repo_lock_list") private val repoLockList: ObservableRepo<List<AppEntry>>,
  @param:Named("cache_purge") private val purgeCache: Cache
) : LockListInteractor, Cache {

  override fun hasShownOnBoarding(): Single<Boolean> = db.hasShownOnBoarding()

  override fun isSystemVisible(): Single<Boolean> = db.isSystemVisible()

  override fun setSystemVisible(visible: Boolean) {
    db.setSystemVisible(visible)
  }

  override fun calculateListDiff(
    oldList: List<AppEntry>,
    newList: List<AppEntry>
  ): Single<ListDiffResult<AppEntry>> = db.calculateListDiff(oldList, newList)
      .doOnError { clearCache() }

  override fun fetchAppEntryList(bypass: Boolean): Observable<List<AppEntry>> {
    val key = "lock-list"
    return repoLockList.get(bypass, key) {
      return@get db.fetchAppEntryList(true)
          // Each time the db refreshes the entire list, clear out the cache to store the new list
          .doOnNext { repoLockList.invalidate(key) }
    }
        .doAfterNext {
          // Cache should only ever hold the most recent list - be memory efficient
          repoLockList.memoryCache()
              .trimToSize(1)
        }
        .doOnError { repoLockList.clearAll() }
  }

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
        .doAfterTerminate { clearCache() }
  }

  override fun clearCache() {
    repoLockList.clearAll()
    purgeCache.clearCache()
  }
}
