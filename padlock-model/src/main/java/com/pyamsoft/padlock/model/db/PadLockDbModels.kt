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

package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult

object PadLockDbModels {

  /**
   * The activity name of the PACKAGE entry in the database
   */
  const val PACKAGE_ACTIVITY_NAME = "PACKAGE"
  const val PACKAGE_EMPTY = "EMPTY"
  const val ACTIVITY_EMPTY = "EMPTY"

  @JvmStatic
  @CheckResult
  fun isEmpty(entry: PadLockEntryModel): Boolean = (PACKAGE_EMPTY == entry.packageName()
      && ACTIVITY_EMPTY == entry.activityName())

  @JvmField
  val EMPTY: PadLockEntryModel = object : PadLockEntryModel {
    override fun packageName(): String {
      return PACKAGE_EMPTY
    }

    override fun activityName(): String {
      return ACTIVITY_EMPTY
    }

    override fun lockCode(): String? {
      return null
    }

    override fun whitelist(): Boolean {
      return false
    }

    override fun systemApplication(): Boolean {
      return false
    }

    override fun ignoreUntilTime(): Long {
      return 0
    }

    override fun lockUntilTime(): Long {
      return 0
    }

  }

}
