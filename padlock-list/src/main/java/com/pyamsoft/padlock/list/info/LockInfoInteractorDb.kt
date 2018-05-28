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

import android.support.annotation.CheckResult
import android.support.v7.util.DiffUtil
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.OnboardingPreferences
import com.pyamsoft.padlock.api.PackageActivityManager
import com.pyamsoft.padlock.api.PadLockDBQuery
import com.pyamsoft.padlock.api.PadLockDBUpdate
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.list.ListDiffResultImpl
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
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
  private val updateDb: PadLockDBUpdate,
  private val modifyInteractor: LockStateModifyInteractor
) : LockInfoInteractor {

  override fun hasShownOnBoarding(): Single<Boolean> {
    return Single.fromCallable { preferences.isInfoDialogOnBoard() }
        .delay(300, TimeUnit.MILLISECONDS)
  }

  @CheckResult
  private fun getLockedActivityEntries(
    name: String
  ): Single<List<PadLockEntryModel.WithPackageNameModel>> = queryDb.queryWithPackageName(name)

  @CheckResult
  private fun getPackageActivities(name: String): Single<List<String>> =
    packageActivityManager.getActivityListForPackage(name)

  @CheckResult
  private fun findMatchingEntry(
    lockEntries: List<PadLockEntryModel.WithPackageNameModel>,
    activityName: String
  ): PadLockEntryModel.WithPackageNameModel? {
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
    var foundEntry: PadLockEntryModel.WithPackageNameModel? = null
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
    padLockEntries: MutableList<PadLockEntryModel.WithPackageNameModel>
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
    foundEntry: PadLockEntryModel.WithPackageNameModel?
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
    entries: List<PadLockEntryModel.WithPackageNameModel>
  ): List<ActivityEntry.Item> {
    // Sort here to avoid stream break
    // If the list is empty, the old flatMap call can hang, causing a list loading error
    // Sort here where we are guaranteed a list of some kind
    val sortedList: MutableList<PadLockEntryModel.WithPackageNameModel> = ArrayList(entries)
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
  private fun activityListZipper(
    fetchName: String
  ): BiFunction<List<String>, List<PadLockEntryModel.WithPackageNameModel>, List<ActivityEntry.Item>> =
    BiFunction { names, entries -> createSortedActivityEntryList(fetchName, names, entries) }

  @CheckResult
  private fun fetchData(fetchName: String): Single<List<ActivityEntry.Item>> {
    return getPackageActivities(fetchName).zipWith(
        getLockedActivityEntries(fetchName), activityListZipper(fetchName)
    )
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
  ): Single<List<ActivityEntry>> {
    return fetchData(packageName)
        .flatMapObservable { Observable.fromIterable(it) }
        .groupBy { it.group }
        .sorted { o1, o2 -> compareByGroup(o1, o2) }
        .concatMap { sortWithinGroup(it) }
        .toList()
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
            return oldItem.packageName == newItem.packageName
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

      }, true)

      return@fromCallable ListDiffResultImpl(newList, result)
    }
  }

  override fun modifySingleDatabaseEntry(
    oldLockState: LockState,
    newLockState: LockState,
    packageName: String,
    activityName: String,
    code: String?,
    system: Boolean
  ): Single<LockState> {
    return modifyInteractor.modifySingleDatabaseEntry(
        oldLockState, newLockState, packageName,
        activityName, code, system
    )
        .flatMap {
          if (it === LockState.NONE) {
            Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated")
            return@flatMap updateExistingEntry(
                packageName, activityName, newLockState === WHITELISTED
            )
          } else {
            Timber.d("Entry handled, just pass through")
            return@flatMap Single.just(it)
          }
        }
  }

  @CheckResult
  private fun updateExistingEntry(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Single<LockState> {
    Timber.d("Entry already exists for: %s %s, update it", packageName, activityName)
    return updateDb.updateWhitelist(packageName, activityName, whitelist)
        .toSingleDefault(if (whitelist) LockState.WHITELISTED else LockState.LOCKED)
  }
}
