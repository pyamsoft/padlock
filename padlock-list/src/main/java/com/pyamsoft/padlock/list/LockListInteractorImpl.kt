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

import androidx.core.util.lruCache
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.cache.Repository
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockListInteractorImpl @Inject internal constructor(
  @param:Named("interactor_lock_list") private val db: LockListInteractor,
  @param:Named("repo_lock_list") private val repoLockList: Repository<List<AppEntry>>,
  @param:Named("cache_purge") private val purgeCache: Cache
) : LockListInteractor, Cache {

  private val cache = lruCache<String, List<AppEntry>>(10)

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
    val cached = cache.get("list")
    return db.fetchAppEntryList(true)
        .doOnNext { cache.put("list", it) }
        .to {
          if (cached == null || bypass) {
            cache.evictAll()
            return@to it
          } else {
            return@to it.startWith(cached)
          }
        }
        .doOnError { clearCache() }
  }

  override fun modifySingleDatabaseEntry(
    oldLockState: LockState,
    newLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Single<LockState> {
    return db.modifySingleDatabaseEntry(
        oldLockState, newLockState, packageName, activityName,
        code, system
    )
        .doAfterTerminate { clearCache() }
  }

  override fun clearCache() {
    repoLockList.clearCache()
    purgeCache.clearCache()
    cache.evictAll()
  }
}
