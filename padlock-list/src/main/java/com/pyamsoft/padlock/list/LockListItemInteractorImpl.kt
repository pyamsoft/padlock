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

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.list.modify.LockStateModifyInteractor
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class LockListItemInteractorImpl @Inject internal constructor(private val cache: Cache,
    private val modifyInteractor: LockStateModifyInteractor) : LockListItemInteractor {

  override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
      packageName: String, activityName: String, code: String?,
      system: Boolean): Single<LockState> {
    return modifyInteractor.modifySingleDatabaseEntry(oldLockState, newLockState, packageName,
        activityName, code, system).doOnSuccess { cache.clearCache() }
  }
}
