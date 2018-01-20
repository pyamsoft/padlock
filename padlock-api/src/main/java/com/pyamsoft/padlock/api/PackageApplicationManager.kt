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

import android.content.pm.ApplicationInfo
import android.support.annotation.CheckResult
import io.reactivex.Single

interface PackageApplicationManager {

    @CheckResult
    fun getActiveApplications(): Single<List<ApplicationItem>>

    @CheckResult
    fun getApplicationInfo(packageName: String): Single<ApplicationItem>

    data class ApplicationItem(val packageName: String, val system: Boolean, val enabled: Boolean) {

        @CheckResult
        fun isEmpty(): Boolean = packageName.isEmpty()

        companion object {

            @JvmField
            val EMPTY = ApplicationItem(
                "",
                false, false
            )

            @CheckResult
            @JvmStatic
            fun fromInfo(info: ApplicationInfo): ApplicationItem =
                ApplicationItem(info.packageName, info.system(), info.enabled)

            @CheckResult
            private fun ApplicationInfo.system(): Boolean = flags and ApplicationInfo.FLAG_SYSTEM != 0
        }
    }
}
