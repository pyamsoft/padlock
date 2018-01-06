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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.api.LockEntryInteractor
import com.pyamsoft.padlock.model.LockWhitelistedEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class LockEntryInteractorCache @Inject internal constructor(
        private val lockWhitelistedBus: EventBus<LockWhitelistedEvent>,
        @param:Named("cache_lock_list") private val lockListCache: Cache,
        @param:Named("cache_lock_info") private val lockInfoCache: Cache,
        @param:Named(
                "interactor_lock_entry") private val impl: LockEntryInteractor) :
        LockEntryInteractor, Cache {

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
            lockCode: String?, isSystem: Boolean, whitelist: Boolean,
            ignoreTime: Long): Completable =
            impl.postUnlock(packageName, activityName, realName, lockCode, isSystem, whitelist,
                    ignoreTime).doOnComplete {
                if (whitelist) {
                    // Clear caches so that views update when we return to them
                    lockListCache.clearCache()
                    lockInfoCache.clearCache()
                    lockWhitelistedBus.publish(
                            LockWhitelistedEvent(packageName,
                                    activityName))
                }
            }

    override fun clearFailCount() = impl.clearFailCount()
}
