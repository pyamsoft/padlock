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
import com.pyamsoft.padlock.model.db.PadLockEntryModel

internal class CreateManager internal constructor(
//  private val factory: PadLockEntryModel.Factory<*>
) {

  @CheckResult
  fun create(
    packageName: String,
    activityName: String,
    lockCode: String?,
    lockUntilTime: Long,
    ignoreUntilTime: Long,
    isSystem: Boolean,
    whitelist: Boolean
  ): PadLockEntryModel {
    TODO()
//    val entry = factory.creator.create(
//        packageName, activityName, lockCode,
//        lockUntilTime, ignoreUntilTime, isSystem, whitelist
//    )
//    if (PadLockDbModels.isEmpty(entry)) {
//      throw RuntimeException("Cannot create EMPTY entry")
//    }
//
//    return entry
  }
}
