/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.model

import android.support.annotation.CheckResult
import com.google.auto.value.AutoValue

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

    val EMPTY: PadLockEntryModel by lazy {
      AutoValue_PadLockEntry(
          PadLockEntry.PACKAGE_EMPTY, PadLockEntry.ACTIVITY_EMPTY, null,
          0, 0, false, false
      )
    }

  }
}
