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

package com.pyamsoft.padlock.api

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel.AllEntriesModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel.WithPackageNameModel
import io.reactivex.Single

interface PadLockDBQuery {

    /**
     * Get either the package with specific name of the PACKAGE entry
     *
     * SQLite only has bindings so we must make do
     * ?1 package name
     * ?2 the PadLock PACKAGE_TAG, see model.PadLockEntry
     * ?3 the specific activity name
     */
    @CheckResult
    fun queryWithPackageActivityNameDefault(
        packageName: String,
        activityName: String
    ): Single<PadLockEntryModel>

    @CheckResult
    fun queryWithPackageName(packageName: String): Single<List<WithPackageNameModel>>

    @CheckResult
    fun queryAll(): Single<List<AllEntriesModel>>
}
