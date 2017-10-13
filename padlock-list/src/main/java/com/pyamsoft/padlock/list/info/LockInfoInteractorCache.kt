/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.list.info

import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class LockInfoInteractorCache @Inject internal constructor(
    @param:Named(
        "interactor_lock_info") private val impl: LockInfoInteractor) : LockInfoInteractor, Cache {

  private var cache: MutableMap<String, Observable<ActivityEntry>?> = HashMap()

  override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
      packageName: String, activityName: String, code: String?,
      system: Boolean): Single<LockState> {
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
              }.doOnError { cache.remove(packageName) })
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
      return@defer cache[packageName]?.doOnError { cache.remove(packageName) }
    }
  }
}

