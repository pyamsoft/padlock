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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.PadLockDBDelete
import com.pyamsoft.padlock.api.PadLockDBQuery
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.api.PackageApplicationManager
import com.pyamsoft.padlock.api.PurgeInteractor
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class PurgeInteractorImpl @Inject internal constructor(
        private val applicationManager: PackageApplicationManager,
        private val deleteDb: PadLockDBDelete,
        private val queryDb: PadLockDBQuery) : PurgeInteractor {

    override fun populateList(forceRefresh: Boolean): Observable<String> {
        return fetchFreshData().flatMapObservable {
            Observable.fromIterable(it)
        }.sorted { obj, str -> obj.compareTo(str, ignoreCase = true) }
    }

    @CheckResult
    private fun getAllEntries(): Single<List<PadLockEntry.AllEntries>> = queryDb.queryAll()

    @CheckResult
    private fun getActiveApplications(): Single<List<String>> = applicationManager.getActiveApplications()
            .flatMapObservable { Observable.fromIterable(it) }
            .map { it.packageName }
            .toSortedList()

    @CheckResult private fun fetchFreshData(): Single<List<String>> {
        return getAllEntries().zipWith(getActiveApplications(),
                BiFunction { allEntries, packageNames ->
                    val mutableAllEntries = allEntries.toMutableList()
                    if (mutableAllEntries.isEmpty()) {
                        Timber.e("Database does not have any AppEntry items")
                        return@BiFunction emptyList()
                    }

                    // Loop through all the package names that we are aware of on the device
                    val foundLocations: MutableSet<PadLockEntry.AllEntries> = HashSet()
                    for (packageName in packageNames) {
                        foundLocations.clear()

                        // Filter out the list to only the package names, add them to foundLocations
                        mutableAllEntries.filterTo(foundLocations) {
                            // If an entry is found in the database remove it
                            it.packageName() == packageName
                        }

                        // Remove all found locations from list
                        mutableAllEntries.removeAll(foundLocations)
                    }

                    // The remaining entries in the database are stale
                    val stalePackageNames: MutableList<String> = ArrayList()
                    mutableAllEntries.mapTo(stalePackageNames) { it.packageName() }
                    return@BiFunction stalePackageNames
                })
    }

    override fun deleteEntry(packageName: String): Single<String> =
            deleteDb.deleteWithPackageName(packageName).andThen(Single.just(packageName))
}
