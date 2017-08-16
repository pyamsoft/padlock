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

package com.pyamsoft.padlock.list.info

import android.app.Activity
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.db.PadLockEntry.WithPackageName
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class LockInfoInteractorImpl @Inject internal constructor(
    private val queryDb: PadLockDBQuery,
    private val packageActivityManager: PackageActivityManager,
    private val preferences: OnboardingPreferences,
    private val lockScreenClass: Class<out Activity>) : LockInfoInteractor {


  override fun hasShownOnBoarding(): Single<Boolean> {
    return Single.fromCallable { preferences.isInfoDialogOnBoard() }
        .delay(300, TimeUnit.MILLISECONDS)
  }

  @CheckResult private fun getLockedActivityEntries(
      name: String): Single<List<PadLockEntry.WithPackageName>> {
    return queryDb.queryWithPackageName(name)
  }

  @CheckResult private fun getPackageActivities(name: String): Single<List<String>> {
    return packageActivityManager.getActivityListForPackage(name)
        .flatMapObservable { Observable.fromIterable(it) }
        .filter { it.equals(lockScreenClass.name, ignoreCase = true).not() }
        .toList()
  }

  @CheckResult private fun findMatchingEntry(lockEntries: MutableList<PadLockEntry.WithPackageName>,
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
    if (pivotPoint.activityName() == activityName) {
      // We are the pivot
      foundEntry = pivotPoint
      start = 0
      end = -1
    } else if (activityName.compareTo(pivotPoint.activityName(), ignoreCase = true) < 0) {
      //  We are before the pivot point
      start = 0
      end = middle - 1
    } else {
      // We are after the pivot point
      start = middle + 1
      end = lockEntries.size - 1
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

  @CheckResult private fun findActivityEntry(activityNames: List<String>,
      padLockEntries: MutableList<PadLockEntry.WithPackageName>, index: Int): ActivityEntry {
    val activityName = activityNames[index]
    val foundEntry = findMatchingEntry(padLockEntries, activityName)
    return createActivityEntry(activityName, foundEntry)
  }

  @CheckResult private fun createActivityEntry(name: String,
      foundEntry: PadLockEntry.WithPackageName?): ActivityEntry {
    val state: LockState
    if (foundEntry == null) {
      state = LockState.DEFAULT
    } else {
      if (foundEntry.whitelist()) {
        state = LockState.WHITELISTED
      } else {
        state = LockState.LOCKED
      }
    }
    return ActivityEntry.builder().name(name).lockState(state).build()
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
          val mutablePadLockEntries: MutableList<WithPackageName> = ArrayList(padLockEntries)

          var start = 0
          var end = activityNames.size - 1

          while (start <= end) {
            // Find entry to compare against
            val entry1 = findActivityEntry(activityNames, mutablePadLockEntries, start)
            activityEntries.add(entry1)

            if (start != end) {
              val entry2 = findActivityEntry(activityNames, mutablePadLockEntries, end)
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
      val entry1Name: String = activityEntry.name()
      val entry2Name: String = activityEntry2.name()

      // Calculate if the starting X characters in the activity name is the exact package name
      var activity1Package: Boolean = false
      if (entry1Name.startsWith(packageName)) {
        val strippedPackageName = entry1Name.replace(packageName, "")
        if (strippedPackageName[0] == '.') {
          activity1Package = true
        }
      }

      var activity2Package: Boolean = false
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
}

