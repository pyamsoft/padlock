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
import com.pyamsoft.padlock.base.db.PadLockDB
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class PurgeInteractor @Inject internal constructor(
    private val packageManagerWrapper: PackageManagerWrapper,
    private val padLockDB: PadLockDB) {

  @JvmField protected var cachedStalePackages: Single<List<String>>? = null

  @CheckResult private fun getActiveApplicationPackageNames(): Single<List<String>> {
    return packageManagerWrapper.getActiveApplications()
        .flatMapObservable { Observable.fromIterable(it) }
        .map { it.packageName }
        .toSortedList()
  }

  @CheckResult private fun getAppEntryList(): Single<List<PadLockEntry.AllEntries>> {
    return padLockDB.queryAll()
  }

  fun clearCache() {
    cachedStalePackages = null
  }

  internal fun populateList(forceRefresh: Boolean): Observable<String> {
    return Single.defer {
      val dataSource: Single<List<String>>
      val cached: Single<List<String>>? = cachedStalePackages
      if (cached == null || forceRefresh) {
        Timber.d("Refresh stale package")
        dataSource = fetchFreshData().cache()
        cachedStalePackages = dataSource
      } else {
        Timber.d("Fetch stale from cache")
        dataSource = cached
      }
      return@defer dataSource
    }.flatMapObservable {
      Observable.fromIterable(it)
    }.sorted { obj, str -> obj.compareTo(str, ignoreCase = true) }
  }

  @CheckResult protected fun fetchFreshData(): Single<List<String>> {
    return getAppEntryList().zipWith(getActiveApplicationPackageNames(),
        BiFunction {
          allEntries, packageNames ->
          val mutableAllEntries: MutableList<PadLockEntry.AllEntries> = ArrayList(allEntries)
          val stalePackageNames: MutableList<String> = ArrayList()
          if (mutableAllEntries.isEmpty()) {
            Timber.e("Database does not have any AppEntry items")
            return@BiFunction stalePackageNames
          }

          // Loop through all the package names that we are aware of on the device
          val foundLocations = HashSet<PadLockEntry.AllEntries>()
          for (packageName in packageNames) {
            foundLocations.clear()

            allEntries.filterTo(foundLocations) {
              // If an entry is found in the database remove it
              it.packageName() == packageName
            }

            mutableAllEntries.removeAll(foundLocations)
          }

          // The remaining entries in the database are stale

          allEntries.mapTo(stalePackageNames) { it.packageName() }

          return@BiFunction stalePackageNames
        })
  }

  @CheckResult internal fun deleteEntry(packageName: String): Single<String> {
    return padLockDB.deleteWithPackageName(packageName)
        .andThen(Completable.fromAction { clearCache() }).andThen(Single.just(packageName))
  }
}
