/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.purge

import com.popinnow.android.repo.Repo
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal class PurgeInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  @Named("interactor_purge") private val db: PurgeInteractor,
  @Named("repo_purge_list") private val repo: Repo<List<String>>
) : PurgeInteractor, Cache {

  override fun clearCache() {
    repo.cancel()
  }

  override fun fetchStalePackageNames(bypass: Boolean): Single<List<String>> {
    return repo.get(bypass) {
      enforcer.assertNotOnMainThread()
      return@get db.fetchStalePackageNames(true)
    }
        .doOnError { clearCache() }
  }

  override fun deleteEntry(packageName: String): Completable = Completable.defer {
    enforcer.assertNotOnMainThread()
    return@defer db.deleteEntry(packageName)
  }
      .doAfterTerminate { clearCache() }
      .doOnError { clearCache() }

  override fun deleteEntries(packageNames: List<String>): Completable = Completable.defer {
    enforcer.assertNotOnMainThread()
    return@defer db.deleteEntries(packageNames)
  }
      .doAfterTerminate { clearCache() }
      .doOnError { clearCache() }
}
