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

package com.pyamsoft.padlock.service.device

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat

object UsagePermissionChecker {

  @JvmStatic
  @CheckResult
  fun hasPermission(context: Context): Boolean {
    val appContext = context.applicationContext
    val appOpsService = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOpsService.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(), appContext.packageName
    )
    val hasPermission: Boolean
    if (mode == AppOpsManager.MODE_DEFAULT) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // On some Marshmallow phones, the default return code means the permission may be controlled by the PM
        val usagePermission =
          ContextCompat.checkSelfPermission(appContext, Manifest.permission.PACKAGE_USAGE_STATS)
        hasPermission = (usagePermission == PackageManager.PERMISSION_GRANTED)
      } else {
        // Otherwise, we listen to app ops manager
        hasPermission = false
      }
    } else {
      hasPermission = (mode == AppOpsManager.MODE_ALLOWED)
    }
    return hasPermission
  }
}
