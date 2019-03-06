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

package com.pyamsoft.padlock.api.lockscreen

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.LockScreenType
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

interface LockScreenInteractor {

  @CheckResult
  fun submitPin(
    packageName: String,
    activityName: String,
    lockCode: String?,
    currentAttempt: String
  ): Single<Boolean>

  @CheckResult
  fun lockOnFailure(
    packageName: String,
    activityName: String
  ): Single<Boolean>

  @CheckResult
  fun getHint(): Single<String>

  @CheckResult
  fun postUnlock(
    packageName: String,
    activityName: String,
    realName: String,
    lockCode: String?,
    isSystem: Boolean,
    whitelist: Boolean,
    ignoreTime: Long
  ): Completable

  fun clearFailCount()

  @CheckResult
  fun getLockScreenType(): Single<LockScreenType>

  @CheckResult
  fun getDefaultIgnoreTime(): Single<Long>

  @CheckResult
  fun getDisplayName(packageName: String): Single<String>

  @CheckResult
  fun isAlreadyUnlocked(
    packageName: String,
    activityName: String
  ): Maybe<Unit>
}
