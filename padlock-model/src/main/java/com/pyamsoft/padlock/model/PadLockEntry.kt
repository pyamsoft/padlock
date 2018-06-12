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

package com.pyamsoft.padlock.model

import androidx.annotation.CheckResult
import com.google.auto.value.AutoValue
import com.pyamsoft.padlock.model.db.PadLockEntryModel

@AutoValue
abstract class PadLockEntry : PadLockEntryModel {

  companion object {

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
    val EMPTY: PadLockEntryModel =
      AutoValue_PadLockEntry(PACKAGE_EMPTY, ACTIVITY_EMPTY, null, 0, 0, false, false)

  }
}
