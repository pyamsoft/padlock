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

package com.pyamsoft.padlock.list.info

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.DiffUtil
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.OnboardingPreferences
import com.pyamsoft.padlock.api.PackageActivityManager
import com.pyamsoft.padlock.api.PadLockDBQuery
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.list.ListDiffResultImpl
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observables.GroupedObservable
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockInfoInteractorDb @Inject internal constructor(
  private val queryDb: PadLockDBQuery,
  private val packageActivityManager: PackageActivityManager,
  private val preferences: OnboardingPreferences,
  private val modifyInteractor: LockStateModifyInteractor
) : LockInfoInteractor {

  override fun hasShownOnBoarding(): Single<Boolean> {
    return Single.fromCallable { preferences.isInfoDialogOnBoard() }
        .delay(300, TimeUnit.MILLISECONDS)
  }

  @CheckResult
  private fun getLockedActivityEntries(
    name: String
  ): Observable<List<WithPackageNameModel>> = queryDb.queryWithPackageName(name)

  @CheckResult
  private fun getPackageActivities(name: String): Single<List<String>> =
    packageActivityManager.getActivityListForPackage(name)

  @CheckResult
  private fun findMatchingEntry(
    lockEntries: List<WithPackageNameModel>,
    activityName: String
  ): WithPackageNameModel? {
    // Short circuit if empty
    if (lockEntries.isEmpty()) {
      return null
    }

    // Select a pivot point
    val middle = lockEntries.size / 2
    val pivotPoint = lockEntries[middle]

    // Compare to pivot
    var start: Int
    var end: Int
    var foundEntry: WithPackageNameModel? = null
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
    return foundEntry
  }

  @CheckResult
  private fun findActivityEntry(
    packageName: String,
    activityName: String,
    padLockEntries: MutableList<WithPackageNameModel>
  ): ActivityEntry.Item {
    val foundEntry = findMatchingEntry(padLockEntries, activityName)

    // Optimize for speed, trade off size
    if (foundEntry != null) {
      padLockEntries.remove(foundEntry)
    }

    return createActivityEntry(packageName, activityName, foundEntry)
  }

  @CheckResult
  private fun createActivityEntry(
    packageName: String,
    name: String,
    foundEntry: WithPackageNameModel?
  ): ActivityEntry.Item {
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
    return ActivityEntry.Item(name, packageName, lockState = state)
  }

  @CheckResult
  private fun createSortedActivityEntryList(
    fetchName: String,
    names: List<String>,
    entries: List<WithPackageNameModel>
  ): List<ActivityEntry.Item> {
    // Sort here to avoid stream break
    // If the list is empty, the old flatMap call can hang, causing a list loading error
    // Sort here where we are guaranteed a list of some kind
    val sortedList: MutableList<WithPackageNameModel> = ArrayList(entries)
    sortedList.sortWith(
        Comparator { o1, o2 ->
          o1.activityName()
              .compareTo(o2.activityName(), ignoreCase = true)
        })

    val activityEntries: MutableList<ActivityEntry.Item> = ArrayList()

    var start = 0
    var end = names.size - 1

    while (start <= end) {
      // Find entry to compare against
      val entry1 = findActivityEntry(fetchName, names[start], sortedList)
      activityEntries.add(entry1)

      if (start != end) {
        val entry2 = findActivityEntry(fetchName, names[end], sortedList)
        activityEntries.add(entry2)
      }

      ++start
      --end
    }

    return activityEntries
  }

  @CheckResult
  private fun fetchData(fetchName: String): Observable<List<ActivityEntry.Item>> {
    return getLockedActivityEntries(fetchName).flatMapSingle { entries ->
      return@flatMapSingle getPackageActivities(fetchName)
          .map { createSortedActivityEntryList(fetchName, it, entries) }
          .flatMapObservable { Observable.fromIterable(it) }
          .toSortedList { o1, o2 ->
            o1.name.compareTo(o2.name, ignoreCase = true)
          }
    }
  }

  @CheckResult
  private fun compareByGroup(
    o1: GroupedObservable<String?, ActivityEntry.Item>,
    o2: GroupedObservable<String?, ActivityEntry.Item>
  ): Int {
    if (o1.key == null && o2.key == null) {
      return 0
    } else if (o1.key == null) {
      return 1
    } else {
      return -1
    }
  }

  @CheckResult
  private fun sortWithinGroup(
    item: GroupedObservable<String?, ActivityEntry.Item>
  ): Observable<ActivityEntry> {
    return item.sorted { o1, o2 -> o1.activity.compareTo(o2.activity, ignoreCase = true) }
        .map { it as ActivityEntry }
        .startWith(ActivityEntry.Group(item.key!!))
  }

  override fun fetchActivityEntryList(
    bypass: Boolean,
    packageName: String
  ): Observable<List<ActivityEntry>> {
    return fetchData(packageName).flatMapSingle {
      return@flatMapSingle Observable.fromIterable(it)
          .groupBy { it.group }
          .sorted { o1, o2 -> compareByGroup(o1, o2) }
          .concatMap { sortWithinGroup(it) }
          .toList()
    }
  }

  override fun calculateListDiff(
    packageName: String,
    oldList: List<ActivityEntry>,
    newList: List<ActivityEntry>
  ): Single<ListDiffResult<ActivityEntry>> {
    return Single.fromCallable {
      val result: DiffUtil.DiffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(
          oldItemPosition: Int,
          newItemPosition: Int
        ): Boolean {
          val oldItem: ActivityEntry = oldList[oldItemPosition]
          val newItem: ActivityEntry = newList[newItemPosition]
          if (oldItem is ActivityEntry.Item && newItem is ActivityEntry.Item) {
            return oldItem.name == newItem.name
          } else if (oldItem is ActivityEntry.Group && newItem is ActivityEntry.Group) {
            return oldItem.name == newItem.name
          } else {
            return false
          }
        }

        override fun areContentsTheSame(
          oldItemPosition: Int,
          newItemPosition: Int
        ): Boolean {
          val oldItem: ActivityEntry = oldList[oldItemPosition]
          val newItem: ActivityEntry = newList[newItemPosition]
          if (oldItem is ActivityEntry.Item && newItem is ActivityEntry.Item) {
            return oldItem == newItem
          } else if (oldItem is ActivityEntry.Group && newItem is ActivityEntry.Group) {
            return oldItem == newItem
          } else {
            return false
          }
        }

        override fun getChangePayload(
          oldItemPosition: Int,
          newItemPosition: Int
        ): Any? {
          // TODO: Construct specific change payload
          Timber.w("TODO: Construct specific change payload")
          return super.getChangePayload(oldItemPosition, newItemPosition)
        }

      }, false)

      return@fromCallable ListDiffResultImpl(newList, result)
    }
  }

  override fun modifyEntry(
    oldLockState: LockState,
    newLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Completable {
    return modifyInteractor.modifyEntry(
        oldLockState, newLockState, packageName,
        activityName, code, system
    )
  }
}
