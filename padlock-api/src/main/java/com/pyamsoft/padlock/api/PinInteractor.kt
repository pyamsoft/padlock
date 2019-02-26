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
import io.reactivex.Single

interface PinInteractor {

  @CheckResult
  fun hasMasterPin(): Single<Boolean>

  @CheckResult
  fun comparePin(attempt: String): Single<Boolean>

  @CheckResult
  fun createPin(
    currentAttempt: String,
    reEntryAttempt: String,
    hint: String
  ): Single<Boolean>

  @CheckResult
  fun clearPin(attempt: String): Single<Boolean>
}
