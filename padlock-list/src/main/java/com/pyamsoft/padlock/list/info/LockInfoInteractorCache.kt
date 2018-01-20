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

import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockInfoUpdater
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LockInfoInteractorCache @Inject internal constructor(
    @param:Named(
        "interactor_lock_info"
    ) private val impl: LockInfoInteractor
) : LockInfoInteractor,
    Cache, LockInfoUpdater {

    private var infoCache: MutableMap<String, Pair<Observable<ActivityEntry>?, Long>> = HashMap()

    override fun modifySingleDatabaseEntry(
        oldLockState: LockState, newLockState: LockState,
        packageName: String, activityName: String, code: String?,
        system: Boolean
    ): Single<LockState> {
        return impl.modifySingleDatabaseEntry(
            oldLockState, newLockState, packageName, activityName,
            code, system
        )
            .doOnSuccess {
                val obj: MutableMap<String, Pair<Observable<ActivityEntry>?, Long>>? = infoCache
                if (obj != null) {
                    val cached: Observable<ActivityEntry>? = obj[packageName]?.first
                    if (cached != null) {
                        obj.put(packageName, Pair(cached.map {
                            if (it.packageName == packageName && it.name == activityName) {
                                return@map ActivityEntry(
                                    name = it.name,
                                    packageName = it.packageName,
                                    lockState = newLockState
                                )
                            } else {
                                return@map it
                            }
                        }, System.currentTimeMillis()))
                    }
                }
            }.doOnError { infoCache.remove(packageName) }
    }

    override fun update(
        packageName: String, activityName: String,
        lockState: LockState
    ): Completable {
        return Completable.fromAction {
            val obj: MutableMap<String, Pair<Observable<ActivityEntry>?, Long>>? = infoCache
            if (obj != null) {
                val pair: Pair<Observable<ActivityEntry>?, Long>? = obj[packageName]
                val cached: Observable<ActivityEntry>? = pair?.first
                val time: Long = pair?.second ?: 0
                if (cached != null && time > 0) {
                    obj.put(packageName, Pair(cached.map {
                        if (it.packageName == packageName && it.name == activityName) {
                            return@map ActivityEntry(
                                name = it.name, packageName = it.packageName,
                                lockState = lockState
                            )
                        } else {
                            return@map it
                        }
                    }, time))
                }
            }
        }
    }

    override fun clearCache() {
        infoCache.clear()
    }

    override fun hasShownOnBoarding(): Single<Boolean> = impl.hasShownOnBoarding()

    override fun populateList(packageName: String, force: Boolean): Observable<ActivityEntry> {
        return Observable.defer {
            val currentTime = System.currentTimeMillis()
            val list: Observable<ActivityEntry>
            val cachedPair = infoCache[packageName]
            val cache: Observable<ActivityEntry>?
            val cachedTime: Long
            if (cachedPair == null) {
                cache = null
                cachedTime = 0L
            } else {
                cache = cachedPair.first
                cachedTime = cachedPair.second
            }
            if (force || cache == null || cachedTime + FIVE_MINUTES_MILLIS < currentTime) {
                list = impl.populateList(packageName, true).cache()
                infoCache.put(packageName, Pair(list, currentTime))
            } else {
                list = cache
            }
            return@defer list
        }.doOnError { infoCache.remove(packageName) }
    }

    companion object {

        private val FIVE_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(5L)
    }
}
