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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.db.AllEntriesImpl
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.PadLockDbEntryImpl
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameImpl
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import com.squareup.sqldelight.runtime.rx.asObservable
import com.squareup.sqldelight.runtime.rx.mapToList
import com.squareup.sqldelight.runtime.rx.mapToOne
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.util.Collections

internal class QueryManager internal constructor(
  private val queries: PadLockEntrySqlQueries,
  private val scheduler: () -> Scheduler
) {

  @CheckResult
  internal fun queryWithPackageActivityNameDefault(
    packageName: String,
    activityName: String
  ): Single<PadLockEntryModel> {
    return queries.withPackageActivityNameDefault(
        packageName, PadLockDbModels.PACKAGE_ACTIVITY_NAME, activityName
    ) { pName, aName, lockCode, lockUntilTime, ignoreUntilTime, systemApplication, whitelist ->
      PadLockDbEntryImpl.create(
          pName, aName, lockCode, whitelist, systemApplication, ignoreUntilTime, lockUntilTime
      )
    }
        .asObservable(scheduler())
        .mapToOne()
        .first(PadLockDbModels.EMPTY)
  }

  @CheckResult
  internal fun queryWithPackageName(
    packageName: String
  ): Observable<List<WithPackageNameModel>> {
    return queries.withPackageName(packageName) { pName, whitelist ->
      WithPackageNameImpl.create(pName, whitelist)
    }
        .asObservable(scheduler())
        .mapToList()
        .map { Collections.unmodifiableList(it) }
  }

  @CheckResult
  internal fun queryAll(): Observable<List<AllEntriesModel>> {
    return queries.allEntries { packageName, activityName, whitelist ->
      AllEntriesImpl.create(packageName, activityName, whitelist)
    }
        .asObservable(scheduler())
        .mapToList()
        .map { Collections.unmodifiableList(it) }
  }
}

