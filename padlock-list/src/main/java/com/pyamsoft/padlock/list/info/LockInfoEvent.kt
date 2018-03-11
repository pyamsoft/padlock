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

package com.pyamsoft.padlock.list.info

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState

sealed class LockInfoEvent {

  data class Modify internal constructor(
    val id: String,
    val name: String,
    val packageName: String,
    val oldState: LockState,
    val newState: LockState,
    val code: String?,
    val system: Boolean
  ) : LockInfoEvent() {

    companion object {

      @JvmStatic
      @CheckResult
      fun from(
        entry: ActivityEntry,
        newState: LockState,
        code: String?,
        system: Boolean
      ): Modify {
        return Modify(
            id = entry.id, name = entry.name, packageName = entry.packageName,
            oldState = entry.lockState, newState = newState, code = code,
            system = system
        )
      }
    }
  }

  sealed class Callback : LockInfoEvent() {

    data class Created(
      val id: String,
      val packageName: String,
      val oldState: LockState
    ) : Callback()

    data class Deleted(
      val id: String,
      val packageName: String,
      val oldState: LockState
    ) : Callback()

    data class Whitelisted(
      val id: String,
      val packageName: String,
      val oldState: LockState
    ) : Callback()

    data class Error(
      val throwable: Throwable,
      val packageName: String
    ) : Callback()
  }
}
