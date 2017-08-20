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

package com.pyamsoft.padlock.purge

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class PurgeInteractorImpl @Inject internal constructor(
    private val applicationManager: PackageApplicationManager,
    private val deleteDb: PadLockDBDelete,
    private val queryDb: PadLockDBQuery) : PurgeInteractor {

  override fun populateList(forceRefresh: Boolean): Observable<String> {
    return fetchFreshData().flatMapObservable {
      Observable.fromIterable(it)
    }.sorted { obj, str -> obj.compareTo(str, ignoreCase = true) }
  }


  @CheckResult
  private fun getAllEntries(): Single<List<PadLockEntry.AllEntries>> = queryDb.queryAll()

  @CheckResult
  private fun getActiveApplications(): Single<List<String>> = applicationManager.getActiveApplications()
      .flatMapObservable { Observable.fromIterable(it) }
      .map { it.packageName }
      .toSortedList()


  @CheckResult private fun fetchFreshData(): Single<List<String>> {
    return getAllEntries().zipWith(getActiveApplications(),
        BiFunction { allEntries, packageNames ->
          val mutableAllEntries: MutableList<PadLockEntry.AllEntries> = ArrayList(allEntries)
          if (mutableAllEntries.isEmpty()) {
            Timber.e("Database does not have any AppEntry items")
            return@BiFunction emptyList()
          }

          // Loop through all the package names that we are aware of on the device
          val foundLocations: MutableSet<PadLockEntry.AllEntries> = HashSet()
          for (packageName in packageNames) {
            foundLocations.clear()

            // Filter out the list to only the package names, add them to foundLocations
            allEntries.filterTo(foundLocations) {
              // If an entry is found in the database remove it
              it.packageName() == packageName
            }

            // Remove all found locations from list
            mutableAllEntries.removeAll(foundLocations)
          }

          // The remaining entries in the database are stale
          val stalePackageNames: MutableList<String> = ArrayList()
          allEntries.mapTo(stalePackageNames) { it.packageName() }
          return@BiFunction stalePackageNames
        })
  }

  override fun deleteEntry(packageName: String): Single<String> =
      deleteDb.deleteWithPackageName(packageName).andThen(Single.just(packageName))
}
