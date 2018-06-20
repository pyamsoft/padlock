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
import com.pyamsoft.padlock.model.db.PadLockDbModels

internal class UpdateManager internal constructor(
  private val queries: PadLockEntrySqlQueries
) {

  @CheckResult
  internal fun updateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Long {
    if (PadLockDbModels.PACKAGE_EMPTY == packageName || PadLockDbModels.ACTIVITY_EMPTY == activityName) {
      throw RuntimeException("Cannot update whitelist EMPTY entry")
    }

    return queries.updateWhitelist(whitelist, packageName, activityName)
  }

  @CheckResult
  internal fun updateIgnoreTime(
    packageName: String,
    activityName: String,
    ignoreTime: Long
  ): Long {
    if (PadLockDbModels.PACKAGE_EMPTY == packageName || PadLockDbModels.ACTIVITY_EMPTY == activityName) {
      throw RuntimeException("Cannot update ignore time EMPTY entry")
    }

    return queries.updateIgnoreUntilTime(ignoreTime, packageName, activityName)
  }

  @CheckResult
  internal fun updateLockTime(
    packageName: String,
    activityName: String,
    lockTime: Long
  ): Long {
    if (PadLockDbModels.PACKAGE_EMPTY == packageName || PadLockDbModels.ACTIVITY_EMPTY == activityName) {
      throw RuntimeException("Cannot update lock time EMPTY entry")
    }

    return queries.updateLockUntilTime(lockTime, packageName, activityName)
  }
}
