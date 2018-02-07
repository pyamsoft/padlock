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
import io.reactivex.Observable
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

  private var cachedList: Observable<String>? = null
  private var lastAccessListTime: Long = 0L

  override fun clearCache() {
    cachedList = null
  }

  override fun populateList(forceRefresh: Boolean): Observable<String> {
    return Observable.defer {
      val cache = cachedList
      val list: Observable<String>
      val currentTime = System.currentTimeMillis()
      if (forceRefresh || cache == null || lastAccessListTime + FIVE_MINUTES_MILLIS < currentTime) {
        list = impl.populateList(true)
            .cache()
        cachedList = list
        lastAccessListTime = currentTime
      } else {
        list = cache
      }
      return@defer list
    }
        .doOnError { clearCache() }
  }

  override fun deleteEntry(packageName: String): Single<String> {
    return impl.deleteEntry(packageName)
        .doOnSuccess {
          val obj: Observable<String>? = cachedList
          if (obj != null) {
            cachedList = obj.filter { it != packageName }
                .doOnError { clearCache() }
          }
        }
  }

  companion object {

    private val FIVE_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(5L)
  }
}
