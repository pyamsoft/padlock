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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.db.PadLockEntry.AllEntries
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager.ApplicationItem
import com.pyamsoft.padlock.base.wrapper.PackageLabelManager
import com.pyamsoft.padlock.list.modify.LockStateModifyInteractor
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton


@Singleton internal class LockListInteractorImpl @Inject internal constructor(
    private val queryDb: PadLockDBQuery,
    private val applicationManager: PackageApplicationManager,
    private val labelManager: PackageLabelManager,
    private val activityManager: PackageActivityManager,
    private val onboardingPreferences: OnboardingPreferences,
    private val modifyInteractor: LockStateModifyInteractor,
    private val preferences: LockListPreferences) : LockListInteractor {

  override fun isSystemVisible(): Single<Boolean> =
      Single.fromCallable { preferences.isSystemVisible() }

  override fun setSystemVisible(visible: Boolean) {
    preferences.setSystemVisible(visible)
  }

  override fun populateList(force: Boolean): Observable<AppEntry> {
    return getValidPackageNames().zipWith(getAppEntryList(),
        BiFunction<List<String>, List<PadLockEntry.AllEntries>, List<LockTuple>> { packageNames, padLockEntries ->
          val lockTuples: MutableList<LockTuple> = ArrayList()
          val copyEntries: MutableList<PadLockEntry.AllEntries> = ArrayList(padLockEntries)
          val copyNames: List<String> = ArrayList(packageNames)
          for (packageName in copyNames) {
            var locked = false
            var whitelist = false
            var hardLocked = false
            val removeEntries = HashSet<AllEntries>()
            for (entry in copyEntries) {
              if (entry.packageName() == packageName) {
                removeEntries.add(entry)
                when {
                  entry.activityName() == PadLockEntry.PACKAGE_ACTIVITY_NAME -> locked = true
                  entry.whitelist() -> whitelist = true
                  else -> hardLocked = true
                }
              }
            }
            copyEntries.removeAll(removeEntries)
            lockTuples.add(LockTuple(packageName, locked, whitelist, hardLocked))
          }
          return@BiFunction lockTuples
        }).flatMapObservable { Observable.fromIterable(it) }
        .flatMapSingle { createFromPackageInfo(it.packageName, it.locked) }
        .toSortedList { o1, o2 ->
          o1.name().compareTo(o2.name(), ignoreCase = true)
        }.flatMapObservable { Observable.fromIterable(it) }
  }

  @CheckResult private fun createFromPackageInfo(packageName: String,
      locked: Boolean): Single<AppEntry> {
    return applicationManager.getApplicationInfo(packageName)
        .flatMap { item ->
          labelManager.loadPackageLabel(item)
              .map {
                AppEntry.builder()
                    .name(it)
                    .packageName(item.packageName)
                    .system(item.system)
                    .locked(locked)
                    .build()
              }
        }
  }

  @CheckResult private fun getActiveApplications(): Observable<ApplicationItem> =
      applicationManager.getActiveApplications().flatMapObservable {
        Observable.fromIterable(it)
      }

  @CheckResult private fun getActivityListForApplication(
      item: ApplicationItem): Single<List<String>> =
      activityManager.getActivityListForPackage(item.packageName)

  @CheckResult private fun getValidPackageNames(): Single<List<String>> {
    return getActiveApplications().flatMapSingle { item ->
      getActivityListForApplication(item).map {
        if (it.isEmpty()) {
          Timber.w("Entry: %s has no activities, hide it", item.packageName)
          return@map ""
        } else {
          return@map item.packageName
        }
      }
    }.filter { it.isNotBlank() }.toList()
  }

  @CheckResult private fun getAppEntryList(): Single<List<PadLockEntry.AllEntries>> =
      queryDb.queryAll()

  override fun hasShownOnBoarding(): Single<Boolean> =
      Single.fromCallable { onboardingPreferences.isListOnBoard() }

  override fun modifySingleDatabaseEntry(oldLockState: LockState, newLockState: LockState,
      packageName: String, activityName: String, code: String?,
      system: Boolean): Single<LockState> {
    return modifyInteractor.modifySingleDatabaseEntry(oldLockState, newLockState, packageName,
        activityName, code, system)
  }

  private data class LockTuple internal constructor(internal val packageName: String,
      internal val locked: Boolean, internal val whitelist: Boolean,
      internal val hardLocked: Boolean)
}

