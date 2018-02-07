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

package com.pyamsoft.padlock.base.db

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqldelight.RowMapper
import io.reactivex.Single
import java.util.Collections

internal class QueryManager internal constructor(private val briteDatabase: BriteDatabase) {

  private val withPackageActivityNameDefaultMapper: RowMapper<out PadLockEntryModel> by lazy {
    PadLockSqlEntry.FACTORY.withPackageActivityNameDefaultMapper()
  }

  private val allEntriesMapper: RowMapper<out PadLockEntryModel.AllEntriesModel> by lazy {
    PadLockSqlEntry.FACTORY.allEntriesMapper { packageName, activityName, whitelist ->
      AutoValue_PadLockSqlEntry_AllEntries(packageName, activityName, whitelist)
    }
  }

  private val withPackageNameMapper: RowMapper<out PadLockEntryModel.WithPackageNameModel> by lazy {
    PadLockSqlEntry.FACTORY.withPackageNameMapper { activityName, whitelist ->
      AutoValue_PadLockSqlEntry_WithPackageName(activityName, whitelist)
    }
  }

  @CheckResult
  internal fun queryWithPackageActivityNameDefault(
      packageName: String,
      activityName: String
  ): Single<PadLockEntryModel> {
    val statement = PadLockSqlEntry.FACTORY.withPackageActivityNameDefault(
        packageName,
        PadLockEntry.PACKAGE_ACTIVITY_NAME, activityName
    )
    return briteDatabase.createQuery(statement)
        .mapToOne { withPackageActivityNameDefaultMapper.map(it) }
        .first(PadLockEntry.EMPTY)
  }

  @CheckResult
  internal fun queryWithPackageName(
      packageName: String
  ): Single<List<PadLockEntryModel.WithPackageNameModel>> {
    val statement = PadLockSqlEntry.FACTORY.withPackageName(packageName)
    return briteDatabase.createQuery(statement)
        .mapToList { withPackageNameMapper.map(it) }
        .first(emptyList())
        .map { Collections.unmodifiableList(it) }
  }

  @CheckResult
  internal fun queryAll(): Single<List<PadLockEntryModel.AllEntriesModel>> {
    val statement = PadLockSqlEntry.FACTORY.allEntries()
    return briteDatabase.createQuery(statement)
        .mapToList { allEntriesMapper.map(it) }
        .first(emptyList())
        .map { Collections.unmodifiableList(it) }
  }
}

