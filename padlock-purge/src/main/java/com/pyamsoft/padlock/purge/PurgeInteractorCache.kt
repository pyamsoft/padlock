/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.purge

import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class PurgeInteractorCache @Inject internal constructor(
    private val impl: PurgeInteractor) : PurgeInteractor, Cache {

  private var cachedList: Observable<String>? = null

  override fun clearCache() {
    cachedList = null
  }

  override fun populateList(forceRefresh: Boolean): Observable<String> {
    return Observable.defer {
      if (forceRefresh || cachedList == null) {
        cachedList = impl.populateList(true).cache()
      }
      return@defer cachedList
    }
  }

  override fun deleteEntry(packageName: String): Single<String> {
    return impl.deleteEntry(packageName).doOnSuccess {
      val obj: Observable<String>? = cachedList
      if (obj != null) {
        cachedList = obj.filter { it == packageName }
      }
    }
  }
}
