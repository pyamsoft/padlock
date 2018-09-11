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

package com.pyamsoft.padlock.api.service

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.preferences.PreferenceWatcher
import com.pyamsoft.padlock.model.ForegroundEvent

interface UsageEventProvider {

  @CheckResult
  fun queryEvents(
    begin: Long,
    end: Long
  ): EventQueryResult

  @CheckResult
  fun watchPermission(func: (Boolean) -> Unit): PreferenceWatcher

  interface EventQueryResult {

    @CheckResult
    fun createForegroundEvent(func: (String, String) -> ForegroundEvent): ForegroundEvent
  }
}
