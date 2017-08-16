/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.list.info

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState

sealed class LockInfoEvent {

  data class Modify(private val entry: ActivityEntry, val newState: LockState,
      val code: String?, val system: Boolean) : LockInfoEvent() {

    @CheckResult fun packageName(): String {
      return entry.packageName()
    }

    @CheckResult fun name(): String {
      return entry.name()
    }

    @CheckResult fun oldState(): LockState {
      return entry.lockState()
    }

    @CheckResult fun id(): String {
      return entry.id()
    }
  }

  sealed class Callback : LockInfoEvent() {

    data class Created(val id: String) : Callback()
    data class Deleted(val id: String) : Callback()
    data class Whitelisted(val id: String) : Callback()
    data class Error(val throwable: Throwable) : Callback()
  }
}

