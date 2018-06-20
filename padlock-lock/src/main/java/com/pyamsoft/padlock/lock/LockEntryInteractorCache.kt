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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.api.LockEntryInteractor
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.cache.Cache
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockEntryInteractorCache @Inject internal constructor(
  private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
  @param:Named("cache_lock_list") private val lockListCache: Cache,
  @param:Named("cache_lock_info") private val lockInfoCache: Cache,
  @param:Named("interactor_lock_entry") private val impl: LockEntryInteractor
) : LockEntryInteractor, Cache {

  override fun clearCache() {
    clearFailCount()
  }

  override fun submitPin(
    packageName: String,
    activityName: String,
    lockCode: String?,
    currentAttempt: String
  ): Single<Boolean> =
    impl.submitPin(packageName, activityName, lockCode, currentAttempt)

  override fun lockEntryOnFail(
    packageName: String,
    activityName: String
  ): Maybe<Long> =
    impl.lockEntryOnFail(packageName, activityName)

  override fun getHint(): Single<String> = impl.getHint()

  override fun postUnlock(
    packageName: String,
    activityName: String,
    realName: String,
    lockCode: String?,
    isSystem: Boolean,
    whitelist: Boolean,
    ignoreTime: Long
  ): Completable =
    impl.postUnlock(
        packageName, activityName, realName, lockCode, isSystem, whitelist,
        ignoreTime
    ).doOnComplete {
      if (whitelist) {
        // Clear caches so that views update when we return to them
        lockListCache.clearCache()
        lockInfoCache.clearCache()
        lockWhitelistedBus.publish(
            LockWhitelistedEvent(
                packageName,
                activityName
            )
        )
      }
    }

  override fun clearFailCount() = impl.clearFailCount()
}
