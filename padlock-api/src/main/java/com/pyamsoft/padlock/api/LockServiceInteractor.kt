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
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.service.RecheckStatus
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import io.reactivex.Flowable
import io.reactivex.Single

interface LockServiceInteractor {

  fun cleanup()

  fun reset()

  fun clearMatchingForegroundEvent(event: ForegroundEvent)

  @CheckResult
  fun isActiveMatching(
    packageName: String,
    className: String
  ): Single<Boolean>

  @CheckResult
  fun listenForForegroundEvents(): Flowable<ForegroundEvent>

  @CheckResult
  fun processEvent(
    packageName: String,
    className: String,
    forcedRecheck: RecheckStatus
  ): Single<PadLockEntryModel>
}
