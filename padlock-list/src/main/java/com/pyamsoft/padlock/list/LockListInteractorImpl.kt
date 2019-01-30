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

package com.pyamsoft.padlock.list

import com.popinnow.android.repo.Repo
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockListUpdatePayload
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal class LockListInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  @param:Named("interactor_lock_list") private val db: LockListInteractor,
  @param:Named("repo_lock_list") private val repo: Repo<List<AppEntry>>
) : LockListInteractor, Cache {

  override fun watchSystemVisible(): Observable<Boolean> {
    return db.watchSystemVisible()
  }

  override fun setSystemVisible(visible: Boolean) {
    db.setSystemVisible(visible)
  }

  override fun fetchAppEntryList(bypass: Boolean): Single<List<AppEntry>> {
    return repo.get(bypass) {
      enforcer.assertNotOnMainThread()
      return@get db.fetchAppEntryList(true)
    }
        .doOnError { clearCache() }
  }

  override fun subscribeForUpdates(provider: ListDiffProvider<AppEntry>): Observable<LockListUpdatePayload> {
    return db.subscribeForUpdates(provider)
        .doOnNext {
          enforcer.assertNotOnMainThread()
          // Each time the updater emits, we get the current list, update it, and cache it
          val list = ArrayList(provider.data())
          list[it.index] = it.entry
          repo.replace(list)
        }
        .doOnError { clearCache() }
  }

  override fun modifyEntry(
    oldLockState: LockState,
    newLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable {
    return db.modifyEntry(
        oldLockState, newLockState, packageName, activityName,
        code, system
    )
        .doOnError { clearCache() }
  }

  override fun clearCache() {
    repo.cancel()
  }
}
