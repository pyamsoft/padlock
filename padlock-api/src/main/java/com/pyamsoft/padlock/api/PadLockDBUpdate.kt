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
import io.reactivex.Completable

interface PadLockDBUpdate {

    @CheckResult
    fun updateLockTime(packageName: String, activityName: String,
            lockUntilTime: Long): Completable

    @CheckResult
    fun updateIgnoreTime(packageName: String, activityName: String,
            ignoreUntilTime: Long): Completable

    @CheckResult
    fun updateWhitelist(packageName: String, activityName: String,
            whitelist: Boolean): Completable
}
