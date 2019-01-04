/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.list

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.LockStateModifyInteractor
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.padlock.api.packagemanager.PackageApplicationManager
import com.pyamsoft.padlock.api.packagemanager.PackageLabelManager
import com.pyamsoft.padlock.api.preferences.LockListPreferences
import com.pyamsoft.padlock.model.ApplicationItem
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.DELETED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.INSERTED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.UPDATED
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockListUpdatePayload
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal class LockListInteractorDb @Inject internal constructor(
  private val enforcer: Enforcer,
  private val queryDao: EntryQueryDao,
  private val applicationManager: PackageApplicationManager,
  private val labelManager: PackageLabelManager,
  private val activityManager: PackageActivityManager,
  private val modifyInteractor: LockStateModifyInteractor,
  private val preferences: LockListPreferences
) : LockListInteractor {

  private fun emit(
    emitter: ObservableEmitter<Boolean>,
    value: Boolean
  ) {
    if (!emitter.isDisposed) {
      emitter.onNext(value)
    }
  }

  override fun watchSystemVisible(): Observable<Boolean> {
    return Observable.defer {
      enforcer.assertNotOnMainThread()
      return@defer Observable.create<Boolean> { emitter ->
        val watcher = preferences.watchSystemVisible { emit(emitter, it) }
        emitter.setCancellable { watcher.stopWatching() }

        enforcer.assertNotOnMainThread()
        emit(emitter, preferences.isSystemVisible())
      }
    }
  }

  override fun setSystemVisible(visible: Boolean) {
    preferences.setSystemVisible(visible)
  }

  @CheckResult
  private fun createNewLockTuple(
    packageName: String,
    copyEntries: MutableList<AllEntriesModel>,
    removeEntries: MutableSet<AllEntriesModel>
  ): LockTuple {
    enforcer.assertNotOnMainThread()

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
    enforcer.assertNotOnMainThread()
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
      enforcer.assertNotOnMainThread()
      return@flatMap getValidPackageNames()
          .map { compileLockedTupleList(it, entries) }
          .flatMapObservable {
            enforcer.assertNotOnMainThread()
            return@flatMapObservable Observable.fromIterable(it)
          }
          .flatMapSingle {
            enforcer.assertNotOnMainThread()
            return@flatMapSingle createFromPackageInfo(it)
          }
          .toSortedList { o1, o2 ->
            o1.name.compareTo(o2.name, ignoreCase = true)
          }
    }
  }

  @CheckResult
  private fun onEntityInserted(
    event: EntityChangeEvent,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    return onEntityInserted(event.packageName!!, event.activityName!!, event.whitelisted, list)
  }

  @CheckResult
  private fun onEntityInserted(
    packageName: String,
    activityName: String,
    whitelisted: Boolean,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
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
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    // One of the sublist items was changed
    val index = list.map { it.packageName }
        .indexOf(packageName)
    if (index < 0) {
      Timber.w("Received a PACKAGE_INSERT event for $packageName - but it's not in the list")
      return Observable.empty()
    }

    val item = list[index].copy()

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

    return Observable.just(LockListUpdatePayload(index, item))
  }

  @CheckResult
  private fun onParentEntityInserted(
    packageName: String,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer applicationManager.getApplicationInfo(packageName)
    }
        .flatMap { item ->
          enforcer.assertNotOnMainThread()
          return@flatMap labelManager.loadPackageLabel(packageName)
              .map { item to it }
        }
        .flatMapObservable { (appInfo, entryName) ->
          enforcer.assertNotOnMainThread()
          return@flatMapObservable createLockListUpdate(list, packageName, appInfo, entryName)
        }
  }

  @CheckResult
  private fun createLockListUpdate(
    list: List<AppEntry>,
    packageName: String,
    appInfo: ApplicationItem,
    entryName: String
  ): Observable<LockListUpdatePayload> {
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
        return Observable.just(LockListUpdatePayload(i, entry.copy(locked = true)))
      }

      // Don't exit out early because our package name may be zzz even though the app name is aaa
    }

    // We are new, add us somewhere
    return Observable.just(
        LockListUpdatePayload(
            index,
            AppEntry(
                entryName,
                packageName,
                appInfo.icon,
                appInfo.system,
                true,
                LinkedHashSet(),
                LinkedHashSet()
            )
        )
    )
  }

  @CheckResult
  private fun onEntityUpdated(
    event: EntityChangeEvent,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    return onEntityUpdated(event.packageName!!, event.activityName!!, event.whitelisted, list)
  }

  @CheckResult
  private fun onEntityUpdated(
    packageName: String,
    activityName: String,
    whitelisted: Boolean,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    val index = list.map { it.packageName }
        .indexOf(packageName)
    if (index < 0) {
      Timber.w("Received a PACKAGE_UPDATE event for $packageName - but it's not in the list")
      return Observable.empty()
    }

    val item = list[index].copy()
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

    return Observable.just(LockListUpdatePayload(index, item))
  }

  @CheckResult
  private fun onEntityDeleted(
    event: EntityChangeEvent,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
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
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    val index = list.map { it.packageName }
        .indexOf(packageName)
    if (index < 0) {
      Timber.w("Received a PACKAGE_DELETE event for $packageName - but it's not in the list")
      return Observable.empty()
    }

    val item: AppEntry
    if (activityName == PadLockDbModels.PACKAGE_ACTIVITY_NAME) {
      // Top level
      item = list[index].copy(locked = false)
    } else {
      // Sub level

      item = list[index].copy()
      // Pop deleted out of whitelisted set
      if (item.whitelisted.contains(activityName)) {
        item.whitelisted.remove(activityName)
      }

      // Pop deleted out of hardlocked set
      if (item.hardLocked.contains(activityName)) {
        item.hardLocked.remove(activityName)
      }
    }

    return Observable.just(LockListUpdatePayload(index, item))
  }

  @CheckResult
  private fun onPackageDeleted(
    packageName: String,
    list: List<AppEntry>
  ): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    return Observable.fromIterable(list
        .filter { it.packageName == packageName }
        .map {
          return@map it.copy(
              locked = false,
              whitelisted = LinkedHashSet(),
              hardLocked = LinkedHashSet()
          )
        }
        .mapIndexed { index, entry -> LockListUpdatePayload(index, entry) }
    )
  }

  @CheckResult
  private fun onAllEntitiesDeleted(list: List<AppEntry>): Observable<LockListUpdatePayload> {
    enforcer.assertNotOnMainThread()
    return Observable.fromIterable(list
        .map {
          it.copy(locked = false, whitelisted = LinkedHashSet(), hardLocked = LinkedHashSet())
        }
        .mapIndexed { index, entry -> LockListUpdatePayload(index, entry) }
    )
  }

  override fun subscribeForUpdates(provider: ListDiffProvider<AppEntry>): Observable<LockListUpdatePayload> {
    return queryDao.subscribeToUpdates()
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

  @CheckResult
  private fun createFromPackageInfo(tuple: LockTuple): Single<AppEntry> {
    return applicationManager.getApplicationInfo(tuple.packageName)
        .flatMap { item ->
          enforcer.assertNotOnMainThread()
          labelManager.loadPackageLabel(item.packageName)
              .map {
                AppEntry(
                    name = it,
                    packageName = item.packageName,
                    icon = item.icon,
                    system = item.system,
                    locked = tuple.locked,
                    whitelisted = tuple.whitelist,
                    hardLocked = tuple.hardLocked
                )
              }
        }
  }

  @CheckResult
  private fun getActiveApplications(): Observable<ApplicationItem> =
    applicationManager.getActiveApplications().flatMapObservable {
      enforcer.assertNotOnMainThread()
      return@flatMapObservable Observable.fromIterable(it)
    }

  @CheckResult
  private fun getActivityListForApplication(
    item: ApplicationItem
  ): Single<List<String>> =
    activityManager.getActivityListForPackage(item.packageName)

  @CheckResult
  private fun getValidPackageNames(): Single<List<String>> {
    return getActiveApplications().flatMapSingle { item ->
      enforcer.assertNotOnMainThread()
      return@flatMapSingle getActivityListForApplication(item)
          .map { if (it.isEmpty()) "" else item.packageName }
    }
        .filter { it.isNotBlank() }
        .toList()
  }

  @CheckResult
  private fun getAllEntries(): Single<List<AllEntriesModel>> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer queryDao.queryAll()
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

  private data class LockTuple internal constructor(
    internal val packageName: String,
    internal val locked: Boolean,
    internal val whitelist: MutableSet<String>,
    internal val hardLocked: MutableSet<String>
  )
}
