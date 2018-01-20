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

import com.google.auto.value.AutoValue
import com.pyamsoft.padlock.model.db.PadLockEntryModel

@AutoValue
internal abstract class PadLockSqlEntry : PadLockEntryModel {

    @AutoValue
    internal abstract class AllEntries : PadLockEntryModel.AllEntriesModel

    @AutoValue
    internal abstract class WithPackageName : PadLockEntryModel.WithPackageNameModel

    companion object {

        internal val FACTORY: PadLockEntryModel.Factory<PadLockSqlEntry> by lazy<PadLockEntryModel.Factory<PadLockSqlEntry>> {
            PadLockEntryModel.Factory { packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime, systemApplication, whitelist ->
                AutoValue_PadLockSqlEntry(
                    packageName, activityName, lockCode, lockUntilTime,
                    ignoreUntilTime,
                    systemApplication, whitelist
                )
            }
        }

    }
}
