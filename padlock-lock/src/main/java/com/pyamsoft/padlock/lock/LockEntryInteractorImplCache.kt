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

package com.pyamsoft.padlock.lock

import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

internal class LockEntryInteractorImplCache(
    private val impl: LockEntryInteractor) : LockEntryInteractor, Cache {

  override fun clearCache() {
    clearFailCount()
  }

  override fun submitPin(packageName: String, activityName: String, lockCode: String?,
      currentAttempt: String): Single<Boolean> =
      impl.submitPin(packageName, activityName, lockCode, currentAttempt)

  override fun lockEntryOnFail(packageName: String, activityName: String): Maybe<Long> =
      impl.lockEntryOnFail(packageName, activityName)

  override fun getHint(): Single<String> = impl.getHint()

  override fun postUnlock(packageName: String, activityName: String, realName: String,
      lockCode: String?, isSystem: Boolean, shouldExclude: Boolean, ignoreTime: Long): Completable =
      impl.postUnlock(packageName, activityName, realName, lockCode, isSystem, shouldExclude,
          ignoreTime)

  override fun clearFailCount() = impl.clearFailCount()

}

