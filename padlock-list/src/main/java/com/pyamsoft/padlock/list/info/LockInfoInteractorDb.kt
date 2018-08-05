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
import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.padlock.api.preferences.OnboardingPreferences
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.DELETED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.INSERTED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.UPDATED
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.LockInfoUpdatePayload
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.list.ListDiffProvider
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
  private val enforcer: Enforcer,
  private val queryDao: EntryQueryDao,
  private val packageActivityManager: PackageActivityManager,
  private val preferences: OnboardingPreferences,
  private val modifyInteractor: LockStateModifyInteractor
) : LockInfoInteractor {

  override fun hasShownOnBoarding(): Single<Boolean> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable preferences.isInfoDialogOnBoard()
    }
        .delay(300, TimeUnit.MILLISECONDS)
  }

  @CheckResult
  private fun getLockedActivityEntries(
    name: String
  ): Single<List<WithPackageNameModel>> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer queryDao.queryWithPackageName(name)
  }

  @CheckResult
  private fun getPackageActivities(name: String): Single<List<String>> =
    packageActivityManager.getActivityListForPackage(name)

  @CheckResult
  private fun findMatchingEntry(
    lockEntries: List<WithPackageNameModel>,
    activityName: String
  ): WithPackageNameModel? {
    enforcer.assertNotOnMainThread()
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
    enforcer.assertNotOnMainThread()
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
    enforcer.assertNotOnMainThread()
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
    enforcer.assertNotOnMainThread()
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
  private fun fetchData(fetchName: String): Single<List<ActivityEntry.Item>> {
    return getLockedActivityEntries(fetchName).flatMap { entries ->
      enforcer.assertNotOnMainThread()
      return@flatMap getPackageActivities(fetchName)
          .map { createSortedActivityEntryList(fetchName, it, entries) }
          .flatMapObservable {
            enforcer.assertNotOnMainThread()
            return@flatMapObservable Observable.fromIterable(it)
          }
          .toSortedList { o1, o2 ->
            o1.name.compareTo(o2.name, ignoreCase = true)
          }
    }
  }

  @CheckResult
  private fun compareByGroup(
    packageName: String,
    o1: GroupedObservable<String?, ActivityEntry.Item>,
    o2: GroupedObservable<String?, ActivityEntry.Item>
  ): Int {
    enforcer.assertNotOnMainThread()
    val o1Key = o1.key
    val o2Key = o2.key
    if (o1Key == null && o2Key == null) {
      // Both are null, equal - but should this be possible?
      return 0
    } else if (o1Key == null) {
      // Left is null, right is higher
      return 1
    } else if (o2Key == null) {
      // Right is null, left is higher
      return -1
    } else {
      // Compare keys alphabetically
      val o1IsPackage = o1Key.startsWith(packageName)
      val o2IsPackage = o2Key.startsWith(packageName)
      if (o1IsPackage && o2IsPackage) {
        // Both within package, compare normally
        return o1Key.compareTo(o2Key)
      } else if (o1IsPackage) {
        // Left in package, it comes first
        return -1
      } else if (o2IsPackage) {
        // Right in package, it comes first
        return 1
      } else {
        // No special treatment, normal compare
        return o1Key.compareTo(o2Key)
      }
    }
  }

  @CheckResult
  private fun sortWithinGroup(
    item: GroupedObservable<String?, ActivityEntry.Item>
  ): Observable<ActivityEntry> {
    enforcer.assertNotOnMainThread()
    return item.sorted { o1, o2 -> o1.activity.compareTo(o2.activity, ignoreCase = true) }
        .map { it as ActivityEntry }
        .startWith(ActivityEntry.Group(item.key!!))
  }

  override fun fetchActivityEntryList(
    bypass: Boolean,
    packageName: String
  ): Single<List<ActivityEntry>> {
    return fetchData(packageName).flatMap { source ->
      enforcer.assertNotOnMainThread()
      return@flatMap Observable.fromIterable(source)
          .groupBy { it.group }
          .sorted { o1, o2 -> compareByGroup(packageName, o1, o2) }
          .concatMap {
            enforcer.assertNotOnMainThread()
            return@concatMap sortWithinGroup(it)
          }
          .toList()
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

  override fun subscribeForUpdates(
    packageName: String,
    provider: ListDiffProvider<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    return queryDao.subscribeToUpdates()
        .filter { it.packageName == packageName }
        .flatMap {
          enforcer.assertNotOnMainThread()
          val oldList = provider.data()
          return@flatMap when (it.type) {
            INSERTED -> onEntityInserted(it, oldList.toList())
            UPDATED -> onEntityUpdated(it, oldList.toList())
            DELETED -> onEntityDeleted(it, oldList.toList())
          }
        }
  }

  private fun onEntityDeleted(
    event: EntityChangeEvent,
    list: List<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    return when {
      event.packageName == null -> onAllEntitiesDeleted(list)
      event.activityName == null -> onPackageDeleted(list)
      else -> onEntryDeleted(/* Never null*/  event.activityName!!, list)
    }
  }

  @CheckResult
  private fun onAllEntitiesDeleted(list: List<ActivityEntry>): Observable<LockInfoUpdatePayload> {
    return onPackageDeleted(list)
  }

  @CheckResult
  private fun onPackageDeleted(
    list: List<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    val result = ArrayList<LockInfoUpdatePayload>()
    for ((index, item) in list.withIndex()) {
      if (item is ActivityEntry.Item) {
        result.add(LockInfoUpdatePayload(index, item.copy(lockState = DEFAULT)))
      }
    }

    return Observable.fromIterable(result)
  }

  @CheckResult
  private fun onEntryDeleted(
    activityName: String,
    list: List<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    var index: Int = -1

    // We must loop manually because filtering out the list changes indexes
    for ((i, item) in list.withIndex()) {
      if (item is ActivityEntry.Item) {
        if (item.name == activityName) {
          index = i
          break
        }
      }
    }

    if (index < 0) {
      Timber.w("Received an ACTIVITY_DELETE event for $activityName but it's not in the list")
      return Observable.empty()
    }

    val item = (list[index] as ActivityEntry.Item).copy(lockState = DEFAULT)
    return Observable.just(LockInfoUpdatePayload(index, item))
  }

  @CheckResult
  private fun onEntityUpdated(
    event: EntityChangeEvent,
    list: List<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    if (event.activityName == null || event.packageName == null) {
      Timber.w("Received an ACTIVITY_INSERT event but will null package name and activity name")
      return Observable.empty()
    }

    val packageName = event.packageName!!
    val activityName = event.activityName!!

    var index: Int = -1

    // We must loop manually because filtering out the list changes indexes
    for ((i, item) in list.withIndex()) {
      if (item is ActivityEntry.Item) {
        if (item.name == activityName) {
          index = i
          break
        }
      }
    }

    if (index < 0) {
      Timber.w(
          """
        |Received an ACTIVITY_UPDATE event for $packageName $activityName
        |but it's not in the list
        |"""
      )
      return Observable.empty()
    }

    if (event.whitelisted) {
      // Whitelisting
      val item = (list[index] as ActivityEntry.Item).copy(lockState = WHITELISTED)
      return Observable.just(LockInfoUpdatePayload(index, item))
    } else {
      // No change
      return Observable.empty()
    }
  }

  @CheckResult
  private fun onEntityInserted(
    event: EntityChangeEvent,
    list: List<ActivityEntry>
  ): Observable<LockInfoUpdatePayload> {
    if (event.activityName == null || event.packageName == null) {
      Timber.w("Received an ACTIVITY_INSERT event but will null package name and activity name")
      return Observable.empty()
    }

    val packageName = event.packageName!!
    val activityName = event.activityName!!

    var index: Int = -1

    // We must loop manually because filtering out the list changes indexes
    for ((i, item) in list.withIndex()) {
      if (item is ActivityEntry.Item) {
        if (item.name == activityName) {
          index = i
          break
        }
      }
    }

    if (index < 0) {
      Timber.w(
          """
        |Received an ACTIVITY_INSERT event for $packageName $activityName
        |but it's not in the list
        |"""
      )
      return Observable.empty()
    }

    val item: ActivityEntry.Item
    if (event.whitelisted) {
      // Whitelisting
      item = (list[index] as ActivityEntry.Item).copy(lockState = WHITELISTED)
    } else {
      // Hardlocked
      item = (list[index] as ActivityEntry.Item).copy(lockState = LOCKED)
    }

    return Observable.just(LockInfoUpdatePayload(index, item))
  }
}
