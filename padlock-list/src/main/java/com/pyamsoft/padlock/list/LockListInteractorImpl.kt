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

import android.support.annotation.CheckResult
import android.support.v7.util.DiffUtil
import com.pyamsoft.padlock.api.*
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.ApplicationItem
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.list.ListDiffResultImpl
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockListInteractorImpl @Inject internal constructor(
    private val queryDb: PadLockDBQuery,
    private val applicationManager: PackageApplicationManager,
    private val labelManager: PackageLabelManager,
    private val activityManager: PackageActivityManager,
    private val onboardingPreferences: OnboardingPreferences,
    private val modifyInteractor: LockStateModifyInteractor,
    private val preferences: LockListPreferences
) : LockListInteractor {

  override fun isSystemVisible(): Single<Boolean> = Single.fromCallable { preferences.isSystemVisible() }

  override fun setSystemVisible(visible: Boolean) {
    preferences.setSystemVisible(visible)
  }

  override fun fetchAppEntryList(force: Boolean): Single<List<AppEntry>> {
    return getValidPackageNames().zipWith(getAppEntryList(),
        BiFunction<List<String>, List<PadLockEntryModel.AllEntriesModel>, List<LockTuple>> { packageNames, padLockEntries ->
          val lockTuples: MutableList<LockTuple> = ArrayList()
          val copyEntries: MutableList<PadLockEntryModel.AllEntriesModel> = ArrayList(
              padLockEntries
          )
          val copyNames: List<String> = ArrayList(packageNames)
          for (packageName in copyNames) {
            var locked = false
            var whitelist = 0
            var hardLocked = 0
            val removeEntries: MutableSet<PadLockEntryModel.AllEntriesModel> = LinkedHashSet()
            for (entry in copyEntries) {
              if (entry.packageName() == packageName) {
                removeEntries.add(entry)
                when {
                  entry.activityName() == PadLockEntry.PACKAGE_ACTIVITY_NAME -> locked = true
                  entry.whitelist() -> ++whitelist
                  else -> ++hardLocked
                }
              }
            }
            copyEntries.removeAll(removeEntries)
            lockTuples.add(LockTuple(packageName, locked, whitelist, hardLocked))
          }
          return@BiFunction lockTuples
        })
        .flatMapObservable { Observable.fromIterable(it) }
        .flatMapSingle { createFromPackageInfo(it) }
        .toSortedList { o1, o2 ->
          o1.name.compareTo(o2.name, ignoreCase = true)
        }
  }

  override fun calculateListDiff(
      oldList: List<AppEntry>,
      newList: List<AppEntry>
  ): Single<ListDiffResult<AppEntry>> {
    return Single.fromCallable {
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
          return oldItem == newItem
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

  @CheckResult
  private fun createFromPackageInfo(tuple: LockTuple): Single<AppEntry> {
    return applicationManager.getApplicationInfo(tuple.packageName)
        .flatMap { item ->
          labelManager.loadPackageLabel(item)
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
      getActivityListForApplication(item).map {
        if (it.isEmpty()) {
          Timber.w("Entry: %s has no activities, hide it", item.packageName)
          return@map ""
        } else {
          return@map item.packageName
        }
      }
    }
        .filter { it.isNotBlank() }
        .toList()
  }

  @CheckResult
  private fun getAppEntryList(): Single<List<PadLockEntryModel.AllEntriesModel>> = queryDb.queryAll()

  override fun hasShownOnBoarding(): Single<Boolean> =
      Single.fromCallable { onboardingPreferences.isListOnBoard() }

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
  }

  private data class LockTuple internal constructor(
      internal val packageName: String,
      internal val locked: Boolean,
      internal val whitelist: Int,
      internal val hardLocked: Int
  )
}
