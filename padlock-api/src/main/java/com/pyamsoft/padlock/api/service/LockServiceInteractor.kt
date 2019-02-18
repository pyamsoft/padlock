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

package com.pyamsoft.padlock.api.service

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.service.ServicePauseState
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

interface LockServiceInteractor {

  fun init()

  fun cleanup()

  fun setPauseState(paused: ServicePauseState)

  @CheckResult
  fun isServiceEnabled(): Single<ServiceState>

  @CheckResult
  fun observeServiceState(): Observable<ServiceState>

  fun clearMatchingForegroundEvent(
    packageName: String,
    className: String
  )

  @CheckResult
  fun ifActiveMatching(
    packageName: String,
    className: String
  ): Maybe<Unit>

  @CheckResult
  fun observeScreenState(): Observable<Boolean>

  @CheckResult
  fun listenForForegroundEvents(): Flowable<ForegroundEvent>

  @CheckResult
  fun processEvent(
    forced: Boolean,
    packageName: String,
    className: String
  ): Single<ProcessedEventPayload>

  enum class ServiceState {
    PAUSED,
    PERMISSION,
    DISABLED,
    ENABLED
  }

  data class ProcessedEventPayload(
    val model: PadLockEntryModel,
    val icon: Int
  )

  data class ForegroundEvent(
    val packageName: String,
    val className: String
  ) {
    companion object {

      @JvmField
      val EMPTY = ForegroundEvent("", "")

      @JvmStatic
      @CheckResult
      fun isEmpty(event: ForegroundEvent): Boolean {
        return event.packageName == EMPTY.packageName || event.className == EMPTY.className
      }

    }
  }
}
