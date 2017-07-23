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

package com.pyamsoft.padlock.list

import android.app.Activity
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDB
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.db.PadLockEntry.WithPackageName
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class LockInfoInteractor @Inject internal constructor(private val padLockDB: PadLockDB,
    private val cacheInteractor: LockInfoCacheInteractor,
    private val packageManagerWrapper: PackageManagerWrapper,
    private val preferences: OnboardingPreferences,
    @param:Named("lockscreen") private val lockScreenClass: Class<out Activity>) {


  @CheckResult fun hasShownOnBoarding(): Single<Boolean> {
    return Single.fromCallable { preferences.isInfoDialogOnBoard() }
        .delay(300, TimeUnit.MILLISECONDS)
  }

  @CheckResult fun populateList(packageName: String,
      forceRefresh: Boolean): Observable<ActivityEntry> {
    return Single.defer {
      val dataSource: Single<MutableList<ActivityEntry>>
      val cached = cacheInteractor.getFromCache(packageName)
      if (cached == null || forceRefresh) {
        Timber.d("Refresh info list data")
        dataSource = fetchFreshData(packageName).cache()
        cacheInteractor.putIntoCache(packageName, dataSource)
      } else {
        Timber.d("Fetch info from cache")
        dataSource = cached
      }
      return@defer dataSource
    }.flatMapObservable { Observable.fromIterable(it) }.sorted { activityEntry, activityEntry2 ->
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

  @CheckResult private fun fetchFreshData(packageName: String): Single<MutableList<ActivityEntry>> {
    return getPackageActivities(packageName).zipWith(getLockedActivityEntries(packageName),
        BiFunction { activityNames, padLockEntries ->
          // Sort here to avoid stream break
          // If the list is empty, the old flatMap call can hang, causing a list loading error
          // Sort here where we are guaranteed a list of some kind
          Collections.sort(padLockEntries)
          { o1, o2 ->
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

  @CheckResult private fun getLockedActivityEntries(
      packageName: String): Single<List<PadLockEntry.WithPackageName>> {
    return padLockDB.queryWithPackageName(packageName)
  }

  @CheckResult private fun getPackageActivities(packageName: String): Single<List<String>> {
    return packageManagerWrapper.getActivityListForPackage(packageName)
        .flatMapObservable { Observable.fromIterable(it) }
        .filter { !it.equals(lockScreenClass.name, ignoreCase = true) }
        .toList()
  }

  @CheckResult private fun findActivityEntry(activityNames: List<String>,
      padLockEntries: MutableList<PadLockEntry.WithPackageName>, index: Int): ActivityEntry {
    val activityName = activityNames[index]
    val foundEntry = findMatchingEntry(padLockEntries, activityName)
    return createActivityEntry(activityName, foundEntry)
  }

  @CheckResult private fun findMatchingEntry(
      padLockEntries: MutableList<PadLockEntry.WithPackageName>,
      activityName: String): PadLockEntry.WithPackageName? {
    if (padLockEntries.isEmpty()) {
      return null
    }

    // Select a pivot point
    val middle = padLockEntries.size / 2
    val pivotPoint = padLockEntries[middle]

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
      end = padLockEntries.size - 1
    }

    while (start <= end) {
      val checkEntry1 = padLockEntries[start++]
      val checkEntry2 = padLockEntries[end--]
      if (activityName == checkEntry1.activityName()) {
        foundEntry = checkEntry1
        break
      } else if (activityName == checkEntry2.activityName()) {
        foundEntry = checkEntry2
        break
      }
    }

    if (foundEntry != null) {
      padLockEntries.remove(foundEntry)
    }

    return foundEntry
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

}

