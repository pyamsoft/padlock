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

package com.pyamsoft.padlock.list

sealed class LockListEvent {

  data class Modify(val packageName: String, val code: String?, val isSystem: Boolean,
      val isChecked: Boolean) : LockListEvent()

  sealed class Callback : LockListEvent() {

    data class Created(val packageName: String) : Callback()
    data class Deleted(val packageName: String) : Callback()
    data class Error(val throwable: Throwable) : Callback()
  }
}