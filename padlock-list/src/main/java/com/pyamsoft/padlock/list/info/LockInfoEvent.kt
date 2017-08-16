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

import com.pyamsoft.padlock.model.LockState

sealed class LockInfoEvent {

  data class Modify(val oldState: LockState, val newState: LockState, val packageName: String,
      val activityName: String, val code: String?, val system: Boolean) : LockInfoEvent()

  sealed class Callback : LockInfoEvent() {

    data class Created(val hook: (String) -> Unit) : Callback()
    data class Deleted(val hook: (String) -> Unit) : Callback()
    data class Whitelisted(val hook: (String) -> Unit) : Callback()
    data class Error(val hook: (Throwable) -> Unit) : Callback()
  }
}

