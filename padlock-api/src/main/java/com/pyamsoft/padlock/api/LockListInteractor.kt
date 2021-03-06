/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.api

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.model.list.LockListUpdatePayload
import io.reactivex.Observable
import io.reactivex.Single

interface LockListInteractor : LockStateModifyInteractor {

  @CheckResult
  fun fetchAppEntryList(bypass: Boolean): Single<List<AppEntry>>

  @CheckResult
  fun subscribeForUpdates(provider: ListDiffProvider<AppEntry>): Observable<LockListUpdatePayload>

  @CheckResult
  fun watchSystemVisible(): Observable<Boolean>

  fun setSystemVisible(visible: Boolean)
}
