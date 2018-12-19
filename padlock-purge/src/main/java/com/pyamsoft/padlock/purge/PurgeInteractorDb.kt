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

package com.pyamsoft.padlock.purge

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.packagemanager.PackageApplicationManager
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal class PurgeInteractorDb @Inject internal constructor(
  private val enforcer: Enforcer,
  private val applicationManager: PackageApplicationManager,
  private val deleteDao: EntryDeleteDao,
  private val queryDao: EntryQueryDao
) : PurgeInteractor {

  override fun fetchStalePackageNames(bypass: Boolean): Single<List<String>> {
    return getAllEntries().flatMap { entries ->
      return@flatMap getActiveApplications()
          .map { filterToStalePackageNames(entries, it) }
          .flatMapObservable {
            enforcer.assertNotOnMainThread()
            return@flatMapObservable Observable.fromIterable(it)
          }
          .toSortedList { s1, s2 -> s1.compareTo(s2, ignoreCase = true) }
    }
  }

  @CheckResult
  private fun getAllEntries(): Single<List<AllEntriesModel>> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer queryDao.queryAll()
  }

  @CheckResult
  private fun getActiveApplications(): Single<List<String>> {
    enforcer.assertNotOnMainThread()
    return applicationManager.getActiveApplications()
        .flatMapObservable {
          enforcer.assertNotOnMainThread()
          return@flatMapObservable Observable.fromIterable(it)
        }
        .map { it.packageName }
        .toSortedList()
  }

  @CheckResult
  private fun filterToStalePackageNames(
    allEntries: List<AllEntriesModel>,
    packageNames: List<String>
  ): List<String> {
    enforcer.assertNotOnMainThread()
    if (allEntries.isEmpty()) {
      Timber.e("Database does not have any AppEntry items")
      return emptyList()
    }

    // Loop through all the package names that we are aware of on the device
    val mutableAllEntries = allEntries.toMutableList()
    val foundLocations = LinkedHashSet<AllEntriesModel>()
    for (packageName in packageNames) {
      foundLocations.clear()

      // Filter out the list to only the package names, add them to foundLocations
      mutableAllEntries.filterTo(foundLocations) {
        // If an entry is found in the database remove it
        it.packageName() == packageName
      }

      // Remove all found locations from list
      mutableAllEntries.removeAll(foundLocations)
    }

    // The remaining entries in the database are stale
    val stalePackageNames = ArrayList<String>()
    mutableAllEntries.mapTo(stalePackageNames) { it.packageName() }
    return stalePackageNames
  }

  override fun deleteEntry(packageName: String): Completable = Completable.defer {
    enforcer.assertNotOnMainThread()
    return@defer deleteDao.deleteWithPackageName(packageName)
  }

  override fun deleteEntries(packageNames: List<String>): Completable {
    return Observable.defer {
      enforcer.assertNotOnMainThread()
      return@defer Observable.fromIterable(packageNames)
    }
        .flatMapCompletable {
          enforcer.assertNotOnMainThread()
          return@flatMapCompletable deleteEntry(it)
        }
  }
}
