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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.cache.Repository
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class PurgeInteractorImpl @Inject internal constructor(
  @Named("interactor_purge") private val db: PurgeInteractor,
  @Named("repo_purge") private val repoStale: Repository<List<String>>
) : PurgeInteractor, Cache {

  override fun clearCache() {
    repoStale.clearCache()
  }

  override fun calculateDiff(
    oldList: List<String>,
    newList: List<String>
  ): Single<ListDiffResult<String>> = db.calculateDiff(oldList, newList)
      .doOnError { clearCache() }

  override fun fetchStalePackageNames(bypass: Boolean): Single<List<String>> {
    return repoStale.get(bypass) { db.fetchStalePackageNames(true) }
        .doOnError { clearCache() }
  }

  override fun deleteEntry(packageName: String): Single<String> =
    db.deleteEntry(packageName).doAfterTerminate { clearCache() }
}
