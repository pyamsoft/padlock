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
import com.pyamsoft.pydroid.list.ListDiffResult
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface PurgeInteractor {

  @CheckResult
  fun fetchStalePackageNames(bypass: Boolean): Observable<List<String>>

  @CheckResult
  fun calculateDiff(
    oldList: List<String>,
    newList: List<String>
  ): Single<ListDiffResult<String>>

  @CheckResult
  fun deleteEntry(packageName: String): Completable

  @CheckResult
  fun deleteEntries(packageNames: List<String>): Completable
}
