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

import android.support.annotation.CheckResult
import android.support.v7.util.DiffUtil
import com.pyamsoft.padlock.api.PackageApplicationManager
import com.pyamsoft.padlock.api.PadLockDBDelete
import com.pyamsoft.padlock.api.PadLockDBQuery
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.list.ListDiffResultImpl
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PurgeInteractorDb @Inject internal constructor(
  private val applicationManager: PackageApplicationManager,
  private val deleteDb: PadLockDBDelete,
  private val queryDb: PadLockDBQuery
) : PurgeInteractor {

  override fun fetchStalePackageNames(bypass: Boolean): Observable<List<String>> {
    return getAllEntries().flatMapSingle { entries ->
      return@flatMapSingle getActiveApplications()
          .map { c(entries, it) }
          .flatMapObservable { Observable.fromIterable(it) }
          .toSortedList { obj, str -> obj.compareTo(str, ignoreCase = true) }
    }
  }

  override fun calculateDiff(
    oldList: List<String>,
    newList: List<String>
  ): Single<ListDiffResult<String>> {
    return Single.fromCallable {
      val result: DiffUtil.DiffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(
          oldItemPosition: Int,
          newItemPosition: Int
        ): Boolean {
          val oldItem: String = oldList[oldItemPosition]
          val newItem: String = newList[newItemPosition]
          return oldItem == newItem
        }

        override fun areContentsTheSame(
          oldItemPosition: Int,
          newItemPosition: Int
        ): Boolean = areItemsTheSame(oldItemPosition, newItemPosition)

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

  @CheckResult
  private fun getAllEntries(): Observable<List<PadLockEntryModel.AllEntriesModel>> =
    queryDb.queryAll()

  @CheckResult
  private fun getActiveApplications(): Single<List<String>> =
    applicationManager.getActiveApplications()
        .flatMapObservable { Observable.fromIterable(it) }
        .map { it.packageName }
        .toSortedList()

  @CheckResult
  private fun c(
    allEntries: List<PadLockEntryModel.AllEntriesModel>,
    packageNames: List<String>
  ): List<String> {
    if (allEntries.isEmpty()) {
      Timber.e("Database does not have any AppEntry items")
      return emptyList()
    }

    // Loop through all the package names that we are aware of on the device
    val mutableAllEntries = allEntries.toMutableList()
    val foundLocations = LinkedHashSet<PadLockEntryModel.AllEntriesModel>()
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

  override fun deleteEntry(packageName: String): Completable =
    deleteDb.deleteWithPackageName(packageName)

  override fun deleteEntries(packageNames: List<String>): Completable {
    return Observable.defer { Observable.fromIterable(packageNames) }
        .flatMapCompletable { deleteEntry(it) }
  }
}
