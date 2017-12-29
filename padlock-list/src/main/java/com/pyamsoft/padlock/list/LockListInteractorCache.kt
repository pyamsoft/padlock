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

package com.pyamsoft.padlock.list

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

@Singleton internal class LockListInteractorCache @Inject internal constructor(
        @param:Named("cache_purge") private val purgeCache: Cache,
        @param:Named(
                "interactor_lock_list") private val impl: LockListInteractor) : LockListInteractor,
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
                list = impl.populateList(true).cache()
                appCache = list
                lastAccessCache = currentTime
            } else {
                list = cache
            }

            return@defer list
        }.doOnError { clearCache() }
    }

    override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
            packageName: String, activityName: String, code: String?,
            system: Boolean): Single<LockState> {
        return impl.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
                code, system)
                .doOnSuccess {
                    val obj: Observable<AppEntry>? = appCache
                    if (obj != null) {
                        appCache = obj.map {
                            if (it.packageName == packageName) {
                                // Update this with the new thing
                                return@map AppEntry(name = it.name, packageName = it.packageName,
                                        locked = newLockState == LOCKED, system = it.system,
                                        whitelisted = it.whitelisted, hardLocked = it.hardLocked)
                            } else {
                                // Pass the original through
                                return@map it
                            }
                        }
                    }
                }.doOnError { clearCache() }
    }

    override fun update(packageName: String, whitelisted: Int, hardLocked: Int): Completable {
        return Completable.fromAction {
            val obj: Observable<AppEntry>? = appCache
            if (obj != null) {
                appCache = obj.map {
                    if (it.packageName == packageName) {
                        return@map AppEntry(name = it.name, packageName = it.packageName,
                                locked = it.locked,
                                system = it.system, whitelisted = whitelisted,
                                hardLocked = hardLocked)
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
