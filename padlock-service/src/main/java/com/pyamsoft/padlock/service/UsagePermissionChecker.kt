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

package com.pyamsoft.padlock.service

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.CheckResult
import android.support.v4.content.ContextCompat

object UsagePermissionChecker {

    @JvmStatic
    @CheckResult
    fun missingUsageStatsPermission(context: Context): Boolean {
        val appContext = context.applicationContext
        val appOpsService: AppOpsManager = appContext.getSystemService(
            Context.APP_OPS_SERVICE
        ) as AppOpsManager
        val mode: Int = appOpsService.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), appContext.packageName
        )
        val missingPermission: Boolean
        if (mode == AppOpsManager.MODE_DEFAULT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // On some Marshmallow phones, the default return code means the permission may be controlled by the PM
                missingPermission = (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.PACKAGE_USAGE_STATS
                ) != PackageManager.PERMISSION_GRANTED)
            } else {
                // Otherwise, we listen to app ops manager
                missingPermission = true
            }
        } else {
            missingPermission = (mode != AppOpsManager.MODE_ALLOWED)
        }
        return missingPermission
    }
}
