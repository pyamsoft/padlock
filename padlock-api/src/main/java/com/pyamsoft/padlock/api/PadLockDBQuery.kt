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

package com.pyamsoft.padlock.api

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import io.reactivex.Single

interface PadLockDBQuery {

  /**
   * Get either the package with specific name of the PACKAGE entry
   *
   * SQLite only has bindings so we must make do
   * ?1 package name
   * ?2 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?3 the specific activity name
   */
  @CheckResult
  fun queryWithPackageActivityNameDefault(
      packageName: String,
      activityName: String
  ): Single<PadLockEntryModel>

  @CheckResult
  fun queryWithPackageName(packageName: String): Single<List<PadLockEntryModel.WithPackageNameModel>>

  @CheckResult
  fun queryAll(): Single<List<PadLockEntryModel.AllEntriesModel>>
}
