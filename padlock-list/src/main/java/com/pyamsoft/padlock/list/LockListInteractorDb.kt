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

package com.pyamsoft.padlock.list

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.DiffUtil
import com.pyamsoft.padlock.api.EntryQueryDao
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.LockListPreferences
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.OnboardingPreferences
import com.pyamsoft.padlock.api.PackageActivityManager
import com.pyamsoft.padlock.api.PackageApplicationManager
import com.pyamsoft.padlock.api.PackageLabelManager
import com.pyamsoft.padlock.model.ApplicationItem
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.DELETED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.INSERTED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.UPDATED
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.list.ListDiffResultImpl
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockListInteractorDb @Inject internal constructor(
  private val queryDao: EntryQueryDao,
  private val applicationManager: PackageApplicationManager,
  private val labelManager: PackageLabelManager,
  private val activityManager: PackageActivityManager,
  private val onboardingPreferences: OnboardingPreferences,
  private val modifyInteractor: LockStateModifyInteractor,
  private val preferences: LockListPreferences
) : LockListInteractor {

  override fun isSystemVisible(): Single<Boolean> =
    Single.fromCallable { preferences.isSystemVisible() }

  override fun setSystemVisible(visible: Boolean) {
    preferences.setSystemVisible(visible)
  }

  @CheckResult
  private fun createNewLockTuple(
    packageName: String,
    copyEntries: MutableList<AllEntriesModel>,
    removeEntries: MutableSet<AllEntriesModel>
  ): LockTuple {

    // We clear out the removal list so it can be reused in a clean state
    removeEntries.clear()

    var locked = false
    val whitelist = LinkedHashSet<String>()
    val hardLocked = LinkedHashSet<String>()
    for (entry in copyEntries) {
      if (entry.packageName() == packageName) {
        when {
          entry.activityName() == PadLockDbModels.PACKAGE_ACTIVITY_NAME -> locked = true
          entry.whitelist() -> whitelist.add(entry.activityName())
          else -> hardLocked.add(entry.activityName())
        }

        // Optimization for speed, trade off size
        // Now that we have found (an consumed) this entry, it will never be used again
        // we can remove it from the set for a smaller iteration loop.
        removeEntries.add(entry)
      }

      // Once the packageName is greater than the one we are looking for, we have passed all
      // possible entries for this package and can stop looping. We will never find another hit.
      if (entry.packageName().compareTo(packageName, ignoreCase = true) > 0) {
        break
      }
    }

    // Optimization for speed, trade off size
    // These entries have been used and will never be used again, we can remove them for a smaller
    // iteration loop
    copyEntries.removeAll(removeEntries)

    // We clear out the removal list so it can be reused in a clean state
    removeEntries.clear()

    return LockTuple(packageName, locked, whitelist, hardLocked)
  }

  @CheckResult
  private fun compileLockedTupleList(
    packageNames: List<String>,
    entries: List<AllEntriesModel>
  ): List<LockTuple> {
    val copyEntries = ArrayList<AllEntriesModel>(entries)
    copyEntries.sortWith(
        Comparator { o1, o2 ->
          return@Comparator o1.packageName()
              .compareTo(o2.packageName(), ignoreCase = true)
        })
    val removeEntries = LinkedHashSet<AllEntriesModel>()
    return packageNames.map { createNewLockTuple(it, copyEntries, removeEntries) }
  }

  override fun fetchAppEntryList(bypass: Boolean): Single<List<AppEntry>> {
    return getAllEntries().flatMap { entries ->
      return@flatMap getValidPackageNames()
          .map { compileLockedTupleList(it, entries) }
          .flatMapObservable { Observable.fromIterable(it) }
          .flatMapSingle { createFromPackageInfo(it) }
          .toSortedList { o1, o2 ->
            o1.name.compareTo(o2.name, ignoreCase = true)
          }
    }
  }

  @CheckResult
  private fun onEntityInserted(
    event: EntityChangeEvent,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    return onEntityInserted(event.packageName!!, event.activityName!!, event.whitelisted, list)
  }

  @CheckResult
  private fun onEntityInserted(
    packageName: String,
    activityName: String,
    whitelisted: Boolean,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    return if (activityName == PadLockDbModels.PACKAGE_ACTIVITY_NAME) {
      onParentEntityInserted(packageName, list)
    } else {
      onSubEntityInserted(packageName, activityName, whitelisted, list)
    }
  }

  @CheckResult
  private fun onSubEntityInserted(
    packageName: String,
    activityName: String,
    whitelisted: Boolean,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    // One of the sublist items was changed
    val index = list.map { it.packageName }
        .indexOf(packageName)
    if (index < 0) {
      Timber.w("Received a PACKAGE_INSERT event for $packageName - but it's not in the list")
      return listOf(-1 to null)
    }

    val item = list[index]

    if (whitelisted) {
      if (item.hardLocked.contains(activityName)) {
        item.hardLocked.remove(activityName)
      }

      item.whitelisted.add(activityName)
    } else {
      if (item.whitelisted.contains(activityName)) {
        item.whitelisted.remove(activityName)
      }

      item.hardLocked.add(activityName)
    }

    return listOf(index to item)
  }

  @CheckResult
  private fun onParentEntityInserted(
    packageName: String,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry>> {
    // We should be in scheduler at this point so it is safe to block off main thread
    val appInfo = applicationManager.getApplicationInfo(packageName)
        .blockingGet()
    val entryName = labelManager.loadPackageLabel(packageName)
        .blockingGet()

    var index = 0
    for ((i, entry) in list.withIndex()) {
      // List is alphabetical, find our spot
      if (entry.name < entryName) {
        // We go after existing items
        index = i + 1
      }

      // Hey look its us (whitelisted or hardlocked entries present)
      if (entry.packageName == packageName) {
        // Return out here for optimization
        return listOf(i to entry.copy(locked = true))
      }

      // Don't exit out early because our package name may be zzz even though the app name is aaa
    }

    // We are new, add us somewhere
    return listOf(
        index to AppEntry(
            entryName,
            packageName,
            appInfo.system,
            true,
            LinkedHashSet(),
            LinkedHashSet()
        )
    )
  }

  @CheckResult
  private fun onEntityUpdated(
    event: EntityChangeEvent,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    return onEntityUpdated(event.packageName!!, event.activityName!!, event.whitelisted, list)
  }

  @CheckResult
  private fun onEntityUpdated(
    packageName: String,
    activityName: String,
    whitelisted: Boolean,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    val index = list.map { it.packageName }
        .indexOf(packageName)
    if (index < 0) {
      Timber.w("Received a PACKAGE_UPDATE event for $packageName - but it's not in the list")
      return listOf(-1 to null)
    }

    val item = list[index]

    if (whitelisted) {
      if (item.hardLocked.contains(activityName)) {
        item.hardLocked.remove(activityName)
      }

      item.whitelisted.add(activityName)
    } else {
      if (item.whitelisted.contains(activityName)) {
        item.whitelisted.remove(activityName)
      }

      // Don't touch hardlock set - if it was already in it stays in.
    }

    return listOf(index to item)
  }

  @CheckResult
  private fun onEntityDeleted(
    event: EntityChangeEvent,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    return when {
      event.packageName == null -> onAllEntitiesDeleted(list)
      event.activityName == null -> onPackageDeleted(/* Never null */ event.packageName!!, list)
      else -> onEntryDeleted(/* Never null*/ event.packageName!!, event.activityName!!, list)
    }
  }

  @CheckResult
  private fun onEntryDeleted(
    packageName: String,
    activityName: String,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry?>> {
    val index = list.map { it.packageName }
        .indexOf(packageName)
    if (index < 0) {
      Timber.w("Received a PACKAGE_DELETE event for $packageName - but it's not in the list")
      return listOf(-1 to null)
    }

    val item = list[index]

    val result: AppEntry
    if (activityName == PadLockDbModels.PACKAGE_ACTIVITY_NAME) {
      // Top level
      result = item.copy(locked = false)
    } else {
      // Sub level

      // Pop deleted out of whitelisted set
      if (item.whitelisted.contains(activityName)) {
        item.whitelisted.remove(activityName)
      }

      // Pop deleted out of hardlocked set
      if (item.hardLocked.contains(activityName)) {
        item.hardLocked.remove(activityName)
      }

      result = item
    }

    return listOf(index to result)
  }

  @CheckResult
  private fun onPackageDeleted(
    packageName: String,
    list: List<AppEntry>
  ): List<Pair<Int, AppEntry>> {
    return list
        .filter { it.packageName == packageName }
        .map {
          return@map it.copy(
              locked = false,
              whitelisted = LinkedHashSet(),
              hardLocked = LinkedHashSet()
          )
        }
        .mapIndexed { index, entry -> index to entry }
  }

  @CheckResult
  private fun onAllEntitiesDeleted(list: List<AppEntry>): List<Pair<Int, AppEntry>> {
    return list
        .map {
          it.copy(locked = false, whitelisted = LinkedHashSet(), hardLocked = LinkedHashSet())
        }
        .mapIndexed { index, entry -> index to entry }
  }

  override fun subscribeForUpdates(diffProvider: ListDiffProvider<AppEntry>):
      Observable<Pair<List<AppEntry>, List<Pair<Int, AppEntry?>>>> {
    return queryDao.subscribeToUpdates()
        .map {
          val oldList = diffProvider.data()
          return@map oldList to when (it.type) {
            INSERTED -> onEntityInserted(it, oldList.toList())
            UPDATED -> onEntityUpdated(it, oldList.toList())
            DELETED -> onEntityDeleted(it, oldList.toList())
          }
        }
  }

  @CheckResult
  private fun calculateListDiff(
    oldList: List<AppEntry>,
    newList: List<AppEntry>
  ): ListDiffResult<AppEntry> {
    val result: DiffUtil.DiffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

      override fun getOldListSize(): Int = oldList.size

      override fun getNewListSize(): Int = newList.size

      override fun areItemsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
      ): Boolean {
        val oldItem: AppEntry = oldList[oldItemPosition]
        val newItem: AppEntry = newList[newItemPosition]
        return oldItem.packageName == newItem.packageName
      }

      override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
      ): Boolean {
        val oldItem: AppEntry = oldList[oldItemPosition]
        val newItem: AppEntry = newList[newItemPosition]
        val same = oldItem == newItem
        Timber.d("Are Contents same? $oldItem, $newItem    $same")
        return same
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

    return ListDiffResultImpl(newList, result)
  }

  @CheckResult
  private fun createFromPackageInfo(tuple: LockTuple): Single<AppEntry> {
    return applicationManager.getApplicationInfo(tuple.packageName)
        .flatMap { item ->
          labelManager.loadPackageLabel(item.packageName)
              .map {
                AppEntry(
                    name = it, packageName = item.packageName,
                    system = item.system,
                    locked = tuple.locked, whitelisted = tuple.whitelist,
                    hardLocked = tuple.hardLocked
                )
              }
        }
  }

  @CheckResult
  private fun getActiveApplications(): Observable<ApplicationItem> =
    applicationManager.getActiveApplications().flatMapObservable {
      Observable.fromIterable(it)
    }

  @CheckResult
  private fun getActivityListForApplication(
    item: ApplicationItem
  ): Single<List<String>> =
    activityManager.getActivityListForPackage(item.packageName)

  @CheckResult
  private fun getValidPackageNames(): Single<List<String>> {
    return getActiveApplications().flatMapSingle { item ->
      return@flatMapSingle getActivityListForApplication(item)
          .map { if (it.isEmpty()) "" else item.packageName }
    }
        .filter { it.isNotBlank() }
        .toList()
  }

  @CheckResult
  private fun getAllEntries(): Single<List<AllEntriesModel>> =
    queryDao.queryAll()

  override fun hasShownOnBoarding(): Single<Boolean> =
    Single.fromCallable { onboardingPreferences.isListOnBoard() }

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

  private data class LockTuple internal constructor(
    internal val packageName: String,
    internal val locked: Boolean,
    internal val whitelist: MutableSet<String>,
    internal val hardLocked: MutableSet<String>
  )
}
