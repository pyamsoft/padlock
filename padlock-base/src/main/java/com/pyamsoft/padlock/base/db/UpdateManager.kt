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

package com.pyamsoft.padlock.base.db

import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.db.PadLockEntryModel.UpdateIgnoreUntilTime
import com.pyamsoft.padlock.model.db.PadLockEntryModel.UpdateLockUntilTime
import com.pyamsoft.padlock.model.db.PadLockEntryModel.UpdateWhitelist
import com.squareup.sqlbrite2.BriteDatabase

internal class UpdateManager internal constructor(
    openHelper: SQLiteOpenHelper,
    private val briteDatabase: BriteDatabase
) {

    private val updateWhitelist by lazy {
        UpdateWhitelist(openHelper.writableDatabase)
    }

    private val updateIgnoreTime by lazy {
        UpdateIgnoreUntilTime(openHelper.writableDatabase)
    }

    private val updateHardLocked by lazy {
        UpdateLockUntilTime(openHelper.writableDatabase)
    }

    @CheckResult
    internal fun updateWhitelist(
        packageName: String, activityName: String,
        whitelist: Boolean
    ): Long {
        if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
            throw RuntimeException("Cannot update whitelist EMPTY entry")
        }

        return briteDatabase.bindAndExecute(updateWhitelist) {
            bind(whitelist, packageName, activityName)
        }
    }

    @CheckResult
    internal fun updateIgnoreTime(
        packageName: String, activityName: String,
        ignoreTime: Long
    ): Long {
        if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
            throw RuntimeException("Cannot update ignore time EMPTY entry")
        }

        return briteDatabase.bindAndExecute(updateIgnoreTime) {
            bind(ignoreTime, packageName, activityName)
        }
    }

    @CheckResult
    internal fun updateLockTime(
        packageName: String, activityName: String,
        lockTime: Long
    ): Long {
        if (PadLockEntry.PACKAGE_EMPTY == packageName || PadLockEntry.ACTIVITY_EMPTY == activityName) {
            throw RuntimeException("Cannot update lock time EMPTY entry")
        }

        return briteDatabase.bindAndExecute(updateHardLocked) {
            bind(lockTime, packageName, activityName)
        }
    }
}