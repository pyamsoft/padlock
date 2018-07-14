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

package com.pyamsoft.padlock.db

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.PadLockDatabaseDelete
import com.pyamsoft.padlock.api.PadLockDatabaseInsert
import com.pyamsoft.padlock.api.PadLockDatabaseQuery
import com.pyamsoft.padlock.api.PadLockDatabaseUpdate
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PadLockDatabaseImpl @Inject internal constructor(context: Context) :
    PadLockDatabaseInsert,
    PadLockDatabaseUpdate,
    PadLockDatabaseQuery,
    PadLockDatabaseDelete {

  private val queryManager: QueryManager
  private val insertManager: InsertManager
  private val deleteManager: DeleteManager
  private val updateManager: UpdateManager

  init {
    val database = PadLockQueryWrapper.create(context)
    val padLockEntrySqlQueries = database.padLockEntrySqlQueries
    queryManager = QueryManager(padLockEntrySqlQueries, Schedulers.io())
    deleteManager = DeleteManager(padLockEntrySqlQueries)
    insertManager = InsertManager(padLockEntrySqlQueries)
    updateManager = UpdateManager(padLockEntrySqlQueries)
  }

  @CheckResult
  private fun deleteWithPackageActivityNameUnguarded(
    packageName: String,
    activityName: String
  ): Long = deleteManager.deleteWithPackageActivity(packageName, activityName)

  @CheckResult
  override fun insert(
    packageName: String,
    activityName: String,
    lockCode: String?,
    lockUntilTime: Long,
    ignoreUntilTime: Long,
    isSystem: Boolean,
    whitelist: Boolean
  ): Completable {
    return Single.fromCallable {
      Timber.i("DB: INSERT $packageName $activityName")
      val deleteResult = deleteWithPackageActivityNameUnguarded(packageName, activityName)
      Timber.d("Delete result: %d", deleteResult)
      return@fromCallable insertManager.insert(
          packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime, isSystem, whitelist
      )
    }
        .ignoreElement()
  }

  override fun updateIgnoreTime(
    packageName: String,
    activityName: String,
    ignoreUntilTime: Long
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: UPDATE IGNORE $packageName $activityName")
      return@fromCallable updateManager.updateIgnoreTime(
          packageName, activityName,
          ignoreUntilTime
      )
    }
  }

  override fun updateLockTime(
    packageName: String,
    activityName: String,
    lockUntilTime: Long
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: UPDATE LOCK $packageName $activityName")
      return@fromCallable updateManager.updateLockTime(
          packageName, activityName,
          lockUntilTime
      )
    }
  }

  override fun updateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: UPDATE WHITELIST $packageName $activityName")
      return@fromCallable updateManager.updateWhitelist(packageName, activityName, whitelist)
    }
  }

  @CheckResult
  override fun queryWithPackageActivityNameDefault(
    packageName: String,
    activityName: String
  ): Single<PadLockEntryModel> {
    Timber.i("DB: QUERY PACKAGE ACTIVITY DEFAULT $packageName $activityName")
    return queryManager.queryWithPackageActivityNameDefault(packageName, activityName)
  }

  @CheckResult
  override fun queryWithPackageName(
    packageName: String
  ): Observable<List<WithPackageNameModel>> {
    return queryManager.queryWithPackageName(packageName)
  }

  @CheckResult
  override fun queryAll(): Observable<List<AllEntriesModel>> {
    return queryManager.queryAll()
  }

  @CheckResult
  override fun deleteWithPackageName(packageName: String): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE $packageName")
      return@fromCallable deleteManager.deleteWithPackage(packageName)
    }
  }

  @CheckResult
  override fun deleteWithPackageActivityName(
    packageName: String,
    activityName: String
  ): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE PACKAGE ACTIVITY $packageName $activityName")
      return@fromCallable deleteWithPackageActivityNameUnguarded(packageName, activityName)
    }
  }

  @CheckResult
  override fun deleteAll(): Completable {
    return Completable.fromCallable {
      Timber.i("DB: DELETE ALL")
      return@fromCallable deleteManager.deleteAll()
    }
  }

  companion object {
  }
}
