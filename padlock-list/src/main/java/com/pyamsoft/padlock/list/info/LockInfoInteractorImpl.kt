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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.db.PadLockEntry.WithPackageName
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.list.modify.LockStateModifyInteractor
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class LockInfoInteractorImpl @Inject internal constructor(
        private val queryDb: PadLockDBQuery,
        private val packageActivityManager: PackageActivityManager,
        private val preferences: OnboardingPreferences,
        private val updateDb: PadLockDBUpdate,
        private val modifyInteractor: LockStateModifyInteractor) :
        LockInfoInteractor {

    override fun hasShownOnBoarding(): Single<Boolean> {
        return Single.fromCallable { preferences.isInfoDialogOnBoard() }
                .delay(300, TimeUnit.MILLISECONDS)
    }

    @CheckResult private fun getLockedActivityEntries(
            name: String): Single<List<PadLockEntry.WithPackageName>> = queryDb.queryWithPackageName(
            name)

    @CheckResult private fun getPackageActivities(name: String): Single<List<String>> =
            packageActivityManager.getActivityListForPackage(name)

    @CheckResult private fun findMatchingEntry(
            lockEntries: MutableList<PadLockEntry.WithPackageName>,
            activityName: String): PadLockEntry.WithPackageName? {
        if (lockEntries.isEmpty()) {
            return null
        }

        // Select a pivot point
        val middle = lockEntries.size / 2
        val pivotPoint = lockEntries[middle]

        // Compare to pivot
        var start: Int
        var end: Int
        var foundEntry: PadLockEntry.WithPackageName? = null
        when {
            pivotPoint.activityName() == activityName -> {
                // We are the pivot
                foundEntry = pivotPoint
                start = 0
                end = -1
            }
            activityName.compareTo(pivotPoint.activityName(), ignoreCase = true) < 0 -> {
                //  We are before the pivot point
                start = 0
                end = middle - 1
            }
            else -> {
                // We are after the pivot point
                start = middle + 1
                end = lockEntries.size - 1
            }
        }

        while (start <= end) {
            val checkEntry1 = lockEntries[start++]
            val checkEntry2 = lockEntries[end--]
            if (activityName == checkEntry1.activityName()) {
                foundEntry = checkEntry1
                break
            } else if (activityName == checkEntry2.activityName()) {
                foundEntry = checkEntry2
                break
            }
        }

        if (foundEntry != null) {
            lockEntries.remove(foundEntry)
        }

        return foundEntry
    }

    @CheckResult private fun findActivityEntry(packageName: String, activityNames: List<String>,
            padLockEntries: MutableList<PadLockEntry.WithPackageName>, index: Int): ActivityEntry {
        val activityName = activityNames[index]
        val foundEntry = findMatchingEntry(padLockEntries, activityName)
        return createActivityEntry(packageName, activityName, foundEntry)
    }

    @CheckResult private fun createActivityEntry(packageName: String, name: String,
            foundEntry: PadLockEntry.WithPackageName?): ActivityEntry {
        val state: LockState = if (foundEntry == null) {
            LockState.DEFAULT
        } else {
            if (foundEntry.whitelist()) {
                LockState.WHITELISTED
            } else {
                LockState.LOCKED
            }
        }
        return ActivityEntry(name = name, packageName = packageName, lockState = state)
    }

    @CheckResult private fun fetchData(fetchName: String): Single<MutableList<ActivityEntry>> {
        return getPackageActivities(fetchName).zipWith(getLockedActivityEntries(fetchName),
                BiFunction { activityNames, padLockEntries ->
                    // Sort here to avoid stream break
                    // If the list is empty, the old flatMap call can hang, causing a list loading error
                    // Sort here where we are guaranteed a list of some kind
                    Collections.sort(padLockEntries) { o1, o2 ->
                        o1.activityName().compareTo(o2.activityName(), ignoreCase = true)
                    }

                    val activityEntries: MutableList<ActivityEntry> = ArrayList()
                    val mutablePadLockEntries: MutableList<WithPackageName> = ArrayList(
                            padLockEntries)

                    var start = 0
                    var end = activityNames.size - 1

                    while (start <= end) {
                        // Find entry to compare against
                        val entry1 = findActivityEntry(fetchName, activityNames,
                                mutablePadLockEntries, start)
                        activityEntries.add(entry1)

                        if (start != end) {
                            val entry2 = findActivityEntry(fetchName, activityNames,
                                    mutablePadLockEntries, end)
                            activityEntries.add(entry2)
                        }

                        ++start
                        --end
                    }

                    return@BiFunction activityEntries
                })

    }

    override fun populateList(packageName: String, force: Boolean): Observable<ActivityEntry> {
        return fetchData(packageName).flatMapObservable {
            Observable.fromIterable(it)
        }.sorted { activityEntry, activityEntry2 ->
            // Package names are all the same
            val entry1Name: String = activityEntry.name
            val entry2Name: String = activityEntry2.name

            // Calculate if the starting X characters in the activity name is the exact package name
            var activity1Package = false
            if (entry1Name.startsWith(packageName)) {
                val strippedPackageName = entry1Name.replace(packageName, "")
                if (strippedPackageName[0] == '.') {
                    activity1Package = true
                }
            }

            var activity2Package = false
            if (entry2Name.startsWith(packageName)) {
                val strippedPackageName = entry2Name.replace(packageName, "")
                if (strippedPackageName[0] == '.') {
                    activity2Package = true
                }
            }
            if (activity1Package && activity2Package) {
                return@sorted entry1Name.compareTo(entry2Name, ignoreCase = true)
            } else if (activity1Package) {
                return@sorted -1
            } else if (activity2Package) {
                return@sorted 1
            } else {
                return@sorted entry1Name.compareTo(entry2Name, ignoreCase = true)
            }
        }
    }

    override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
            packageName: String, activityName: String, code: String?,
            system: Boolean): Single<LockState> {
        return modifyInteractor.modifySingleDatabaseEntry(oldLockState, newLockState, packageName,
                activityName, code, system)
                .flatMap {
                    if (it === LockState.NONE) {
                        Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated")

                        // Assigned to resultState
                        return@flatMap updateExistingEntry(packageName, activityName,
                                newLockState === WHITELISTED)
                    } else {
                        Timber.d("Entry handled, just pass through")

                        // Assigned to resultState
                        return@flatMap Single.just(it)
                    }
                }
    }

    @CheckResult private fun updateExistingEntry(
            packageName: String, activityName: String, whitelist: Boolean): Single<LockState> {
        Timber.d("Entry already exists for: %s %s, update it", packageName, activityName)
        return updateDb.updateWhitelist(whitelist, packageName, activityName)
                .toSingleDefault(if (whitelist) LockState.WHITELISTED else LockState.LOCKED)
    }
}

