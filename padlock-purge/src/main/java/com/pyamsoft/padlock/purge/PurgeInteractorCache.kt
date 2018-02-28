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
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class PurgeInteractorCache @Inject internal constructor(
    @param:Named("interactor_purge") private val impl: PurgeInteractor
) : PurgeInteractor,
    Cache {

  private var cachedList: Single<MutableList<String>>? = null
  private var lastAccessListTime: Long = 0L

  override fun clearCache() {
    cachedList = null
  }

  override fun calculateDiff(
      oldList: List<String>,
      newList: List<String>
  ): Single<ListDiffResult<String>> =
      impl.calculateDiff(oldList, newList).doOnError { clearCache() }

  override fun fetchStalePackageNames(forceRefresh: Boolean): Single<List<String>> {
    return Single.defer {
      val cache: Single<MutableList<String>>? = cachedList
      val list: Single<List<String>>
      val currentTime = System.currentTimeMillis()
      if (forceRefresh || cache == null || lastAccessListTime + FIVE_MINUTES_MILLIS < currentTime) {
        list = impl.fetchStalePackageNames(true)
            .cache()
        cachedList = list.map { it.toMutableList() }
        lastAccessListTime = currentTime
      } else {
        list = cache.map { it.toList() }
      }
      return@defer list
    }
        .doOnError { clearCache() }
  }

  override fun deleteEntry(packageName: String): Single<String> {
    return impl.deleteEntry(packageName)
        .doOnSuccess {
          val obj: Single<MutableList<String>>? = cachedList
          if (obj != null) {
            cachedList = obj.doOnSuccess { it.remove(packageName) }
                .doOnError { clearCache() }
          }
        }
  }

  companion object {

    private val FIVE_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(5L)
  }
}
