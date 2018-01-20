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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.pydroid.data.Cache
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class PurgeInteractorCache @Inject internal constructor(
    @param:Named("interactor_purge") private val impl: PurgeInteractor
) : PurgeInteractor,
    Cache {

    private var cachedList: Observable<String>? = null
    private var lastAccessListTime: Long = 0L

    override fun clearCache() {
        cachedList = null
    }

    override fun populateList(forceRefresh: Boolean): Observable<String> {
        return Observable.defer {
            val cache = cachedList
            val list: Observable<String>
            val currentTime = System.currentTimeMillis()
            if (forceRefresh || cache == null || lastAccessListTime + FIVE_MINUTES_MILLIS < currentTime) {
                list = impl.populateList(true).cache()
                cachedList = list
                lastAccessListTime = currentTime
            } else {
                list = cache
            }
            return@defer list
        }.doOnError { clearCache() }
    }

    override fun deleteEntry(packageName: String): Single<String> {
        return impl.deleteEntry(packageName).doOnSuccess {
            val obj: Observable<String>? = cachedList
            if (obj != null) {
                cachedList = obj.filter { it != packageName }.doOnError { clearCache() }
            }
        }
    }

    companion object {

        private val FIVE_MINUTES_MILLIS = TimeUnit.MINUTES.toMillis(5L)
    }
}
