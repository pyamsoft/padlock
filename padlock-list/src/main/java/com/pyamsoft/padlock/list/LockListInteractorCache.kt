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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.LockListUpdater
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.cache.TimedEntry
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockListInteractorCache @Inject internal constructor(
  @param:Named("cache_purge") private val purgeCache: Cache,
  @param:Named("interactor_lock_list") private val impl: LockListInteractor
) : LockListInteractor, Cache, LockListUpdater {

  private val appCache = TimedEntry<Single<MutableList<AppEntry>>>()

  override fun hasShownOnBoarding(): Single<Boolean> = impl.hasShownOnBoarding()

  override fun isSystemVisible(): Single<Boolean> = impl.isSystemVisible()

  override fun setSystemVisible(visible: Boolean) {
    impl.setSystemVisible(visible)
  }

  override fun calculateListDiff(
    oldList: List<AppEntry>,
    newList: List<AppEntry>
  ): Single<ListDiffResult<AppEntry>> = impl.calculateListDiff(oldList, newList)
      .doOnError { clearCache() }

  override fun fetchAppEntryList(force: Boolean): Single<List<AppEntry>> {
    return appCache.getElseFresh(force) {
      impl.fetchAppEntryList(true)
          .map { it.toMutableList() }
          .cache()
    }
        .map { it.toList() }
        .doOnError { clearCache() }
  }

  @CheckResult
  private fun copyEntryWithState(
    entry: AppEntry,
    locked: Boolean = entry.locked,
    whitelisted: Int = entry.whitelisted,
    hardLocked: Int = entry.hardLocked
  ): AppEntry = entry.copy(locked = locked, whitelisted = whitelisted, hardLocked = hardLocked)

  private fun MutableList<AppEntry>.updateAppEntry(
    packageName: String,
    func: (AppEntry) -> AppEntry
  ) {
    for ((index, entry) in this.withIndex()) {
      if (entry.packageName == packageName) {
        this[index] = func(entry)
        break
      }
    }
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
          appCache.updateIfAvailable { single ->
            single.doOnSuccess {
              it.updateAppEntry(packageName) { copyEntryWithState(it, newLockState == LOCKED) }
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
      appCache.updateIfAvailable { single ->
        single.doOnSuccess {
          it.updateAppEntry(packageName) {
            copyEntryWithState(it, whitelisted = whitelisted, hardLocked = hardLocked)
          }
        }
      }
    }
        .doOnError { clearCache() }
  }

  override fun clearCache() {
    appCache.clearCache()
    purgeCache.clearCache()
  }
}
