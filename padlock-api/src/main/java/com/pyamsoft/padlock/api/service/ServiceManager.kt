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

package com.pyamsoft.padlock.api.service

import android.app.PendingIntent
import androidx.annotation.CheckResult

interface ServiceManager {

  fun startService(restart: Boolean)

  @CheckResult
  fun fireMainActivityIntent(forceRefreshOnOpen: Boolean): PendingIntent

  @CheckResult
  fun fireStartIntent(): PendingIntent

  @CheckResult
  fun fireUserPauseIntent(): PendingIntent

  @CheckResult
  fun fireTempPauseIntent(): PendingIntent

  companion object {

    const val SERVICE_COMMAND = "SERVICE_COMMAND"
    const val FORCE_REFRESH_ON_OPEN = "FORCE_REFRESH_ON_OPEN"

  }

  enum class Commands {
    START,
    PAUSE,
    TEMP_PAUSE,
    USER_PAUSE,
    USER_TEMP_PAUSE
  }

}
