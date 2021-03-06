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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.model.LockScreenType
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named

internal class LockScreenInteractorCache @Inject internal constructor(
  private val enforcer: Enforcer,
  @param:Named("cache_lock_list") private val lockListCache: Cache,
  @param:Named("cache_lock_info") private val lockInfoCache: Cache,
  @param:Named("interactor_lock_screen") private val impl: LockScreenInteractor
) : LockScreenInteractor, Cache {

  override fun clearCache() {
    clearFailCount()
  }

  override fun submitPin(
    packageName: String,
    activityName: String,
    lockCode: String?,
    currentAttempt: String
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer impl.submitPin(packageName, activityName, lockCode, currentAttempt)
    }
  }

  override fun lockOnFailure(
    packageName: String,
    activityName: String
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer impl.lockOnFailure(packageName, activityName)
    }
  }

  override fun getHint(): Single<String> = impl.getHint()

  override fun postUnlock(
    packageName: String,
    activityName: String,
    realName: String,
    lockCode: String?,
    isSystem: Boolean,
    whitelist: Boolean,
    ignoreTime: Long
  ): Completable {
    return impl.postUnlock(
        packageName, activityName, realName,
        lockCode, isSystem, whitelist, ignoreTime
    )
        .doOnComplete {
          enforcer.assertNotOnMainThread()

          if (whitelist) {
            // Clear caches so that views update when we return to them
            lockListCache.clearCache()
            lockInfoCache.clearCache()
          }
        }
  }

  override fun clearFailCount() {
    impl.clearFailCount()
  }

  override fun getDefaultIgnoreTime(): Single<Long> {
    return impl.getDefaultIgnoreTime()
  }

  override fun getDisplayName(packageName: String): Single<String> {
    return impl.getDisplayName(packageName)
  }

  override fun getLockScreenType(): Single<LockScreenType> {
    return impl.getLockScreenType()
  }

  override fun isAlreadyUnlocked(
    packageName: String,
    activityName: String
  ): Maybe<Unit> {
    return impl.isAlreadyUnlocked(packageName, activityName)
  }
}
