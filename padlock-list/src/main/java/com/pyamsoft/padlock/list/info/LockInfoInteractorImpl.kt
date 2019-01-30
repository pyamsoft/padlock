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

package com.pyamsoft.padlock.list.info

import com.popinnow.android.repo.MultiRepo
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockInfoUpdatePayload
import com.pyamsoft.pydroid.core.cache.Cache
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal class LockInfoInteractorImpl @Inject internal constructor(
  @Named("interactor_lock_info") private val db: LockInfoInteractor,
  @Named("repo_lock_info") private val repo: MultiRepo<List<ActivityEntry>>
) : LockInfoInteractor, Cache {

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
        .doOnError { repo.clear(packageName) }
  }

  override fun clearCache() {
    repo.cancel()
  }

  override fun subscribeForUpdates(
    packageName: String,
    provider: ListDiffProvider<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    return db.subscribeForUpdates(packageName, provider)
        .doOnNext {
          // Each time the updater emits, we get the current list, update it, and cache it
          val list = ArrayList(provider.data())
          list[it.index] = it.entry
          repo.replace(packageName, list)
        }
        .doOnError { repo.clear(packageName) }
  }

  override fun fetchActivityEntryList(
    bypass: Boolean,
    packageName: String
  ): Single<List<ActivityEntry>> {
    return repo.get(packageName, bypass) { db.fetchActivityEntryList(true, packageName) }
        .doOnError { repo.clear(packageName) }
  }

}
