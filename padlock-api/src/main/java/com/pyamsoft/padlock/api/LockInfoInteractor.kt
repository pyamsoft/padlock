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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.LockInfoUpdatePayload
import com.pyamsoft.pydroid.list.ListDiffProvider
import io.reactivex.Observable
import io.reactivex.Single

interface LockInfoInteractor : LockStateModifyInteractor {

  @CheckResult
  fun subscribeForUpdates(
    packageName: String,
    provider: ListDiffProvider<ActivityEntry>
  ): Observable<LockInfoUpdatePayload>

  @CheckResult
  fun hasShownOnBoarding(): Single<Boolean>

  @CheckResult
  fun fetchActivityEntryList(
    bypass: Boolean,
    packageName: String
  ): Single<List<ActivityEntry>>
}
