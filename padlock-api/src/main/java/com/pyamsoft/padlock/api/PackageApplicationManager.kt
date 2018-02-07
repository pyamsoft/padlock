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

package com.pyamsoft.padlock.api

import android.content.pm.ApplicationInfo
import android.support.annotation.CheckResult
import io.reactivex.Single

interface PackageApplicationManager {

  @CheckResult
  fun getActiveApplications(): Single<List<ApplicationItem>>

  @CheckResult
  fun getApplicationInfo(packageName: String): Single<ApplicationItem>

  data class ApplicationItem(
      val packageName: String,
      val system: Boolean,
      val enabled: Boolean
  ) {

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
