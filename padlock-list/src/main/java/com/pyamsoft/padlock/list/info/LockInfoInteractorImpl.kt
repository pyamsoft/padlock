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
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.cache.RepositoryMap
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@JvmSuppressWildcards
@Singleton
internal class LockInfoInteractorImpl @Inject internal constructor(
  @Named("interactor_lock_info") private val db: LockInfoInteractor,
  @Named(
      "repo_lock_info"
  ) private val repoLockInfo: RepositoryMap<String, List<ActivityEntry>>
) : LockInfoInteractor, Cache {

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
        .doAfterTerminate { repoLockInfo.remove(packageName) }
  }

  override fun clearCache() {
    repoLockInfo.clearCache()
  }

  override fun hasShownOnBoarding(): Single<Boolean> = db.hasShownOnBoarding()

  override fun fetchActivityEntryList(
    bypass: Boolean,
    packageName: String
  ): Single<List<ActivityEntry>> {
    return Single.defer {
      Maybe.concat(
          repoLockInfo.get(bypass, packageName),
          db.fetchActivityEntryList(bypass, packageName).doOnSuccess {
            repoLockInfo.set(packageName, it)
          }.toMaybe()
      )
          .firstOrError()
    }
        .doAfterTerminate { repoLockInfo.remove(packageName) }
  }

  override fun calculateListDiff(
    packageName: String,
    oldList: List<ActivityEntry>,
    newList: List<ActivityEntry>
  ): Single<ListDiffResult<ActivityEntry>> =
    db.calculateListDiff(packageName, oldList, newList)
        .doOnError { repoLockInfo.remove(packageName) }

}
