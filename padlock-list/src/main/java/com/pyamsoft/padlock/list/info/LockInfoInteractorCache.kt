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

package com.pyamsoft.padlock.list.info

import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class LockInfoInteractorCache @Inject internal constructor(
    private val impl: LockInfoInteractor) : LockInfoInteractor, Cache {

  private var cache: MutableMap<String, Observable<ActivityEntry>?> = HashMap()

  override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
      packageName: String, activityName: String, code: String?, system: Boolean): Maybe<LockState> {
    return impl.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
        code, system)
        .doOnSuccess {
          val obj: MutableMap<String, Observable<ActivityEntry>?>? = cache
          if (obj != null) {
            val cached: Observable<ActivityEntry>? = obj[packageName]
            if (cached != null) {
              obj.put(packageName, cached.map {
                if (it.packageName() == packageName && it.name() == activityName) {
                  return@map it.toBuilder().lockState(newLockState).build()
                } else {
                  return@map it
                }
              })
            }
          }
        }
  }

  override fun clearCache() {
    cache.clear()
  }

  override fun hasShownOnBoarding(): Single<Boolean> = impl.hasShownOnBoarding()

  override fun populateList(packageName: String, force: Boolean): Observable<ActivityEntry> {
    return Observable.defer {
      if (force || cache[packageName] == null) {
        cache.put(packageName, impl.populateList(packageName, true).cache())
      }
      return@defer cache[packageName]
    }
  }
}

