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

import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.LockListUpdater
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockListInteractorCache @Inject internal constructor(
    @param:Named("cache_purge") private val purgeCache: Cache,
    @param:Named(
        "interactor_lock_list"
    ) private val impl: LockListInteractor
) : LockListInteractor,
    Cache, LockListUpdater {

  private var appCache: Observable<AppEntry>? = null
  private var lastAccessCache: Long = 0L

  override fun hasShownOnBoarding(): Single<Boolean> = impl.hasShownOnBoarding()

  override fun isSystemVisible(): Single<Boolean> = impl.isSystemVisible()

  override fun setSystemVisible(visible: Boolean) {
    impl.setSystemVisible(visible)
  }

  override fun populateList(force: Boolean): Observable<AppEntry> {
    return Observable.defer {
      val cache = appCache
      val currentTime = System.currentTimeMillis()
      val list: Observable<AppEntry>
      if (force || cache == null || lastAccessCache + FIVE_MINUTES_MILLIS < currentTime) {
        list = impl.populateList(true)
            .cache()
        appCache = list
        lastAccessCache = currentTime
      } else {
        list = cache
      }

      return@defer list
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
    return impl.modifySingleDatabaseEntry(
        oldLockState, newLockState, packageName, activityName,
        code, system
    )
        .doOnSuccess {
          val obj: Observable<AppEntry>? = appCache
          if (obj != null) {
            appCache = obj.map {
              if (it.packageName == packageName) {
                // Update this with the new thing
                return@map AppEntry(
                    name = it.name, packageName = it.packageName,
                    locked = newLockState == LOCKED, system = it.system,
                    whitelisted = it.whitelisted, hardLocked = it.hardLocked
                )
              } else {
                // Pass the original through
                return@map it
              }
            }
          }
        }
        .doOnError { clearCache() }
  }

  override fun update(
      packageName: String,
      whitelisted: Int,
      hardLocked: Int
  ): Completable {
    return Completable.fromAction {
      val obj: Observable<AppEntry>? = appCache
      if (obj != null) {
        appCache = obj.map {
          if (it.packageName == packageName) {
            return@map AppEntry(
                name = it.name, packageName = it.packageName,
                locked = it.locked,
                system = it.system, whitelisted = whitelisted,
                hardLocked = hardLocked
            )
          } else {
            // Pass the original through
            return@map it
          }
        }
      }
    }
  }

  override fun clearCache() {
    appCache = null
    purgeCache.clearCache()
  }

  companion object {

    private val FIVE_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(5L)
  }
}
