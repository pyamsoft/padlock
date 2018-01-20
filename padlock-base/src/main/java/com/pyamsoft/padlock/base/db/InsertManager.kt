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
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel.InsertEntry
import com.squareup.sqlbrite2.BriteDatabase

internal class InsertManager internal constructor(
    openHelper: SQLiteOpenHelper,
    private val briteDatabase: BriteDatabase
) {

    private val insertEntry by lazy {
        InsertEntry(openHelper.writableDatabase)
    }

    @CheckResult
    internal fun insert(entry: PadLockEntryModel): Long {
        if (PadLockEntry.isEmpty(entry)) {
            throw RuntimeException("Cannot insert EMPTY entry")
        }

        return briteDatabase.bindAndExecute(insertEntry) {
            bind(
                entry.packageName(), entry.activityName(), entry.lockCode(), entry.lockUntilTime(),
                entry.ignoreUntilTime(), entry.systemApplication(), entry.whitelist()
            )
        }
    }
}