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

package com.pyamsoft.padlock.model.list

import com.pyamsoft.padlock.model.LockState

sealed class ActivityEntry(name: String) {

  val group: String

  init {
    val lastIndex = name.lastIndexOf('.')
    if (lastIndex > 0) {
      group = name.substring(0 until lastIndex)
    } else {
      group = name
    }
  }

  data class Group(val name: String) : ActivityEntry(name)

  data class Item(
    val name: String,
    val packageName: String,
    val lockState: LockState
  ) : ActivityEntry(name) {

    val id: String = "$packageName|$name"
    val activity: String

    init {
      val lastIndex = name.lastIndexOf('.')
      activity = name.substring(lastIndex + 1)
    }
  }

}

