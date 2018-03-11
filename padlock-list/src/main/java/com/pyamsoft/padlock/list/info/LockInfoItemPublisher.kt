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

import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.pydroid.bus.EventBus
import javax.inject.Inject

class LockInfoItemPublisher @Inject internal constructor(private val bus: EventBus<LockInfoEvent>) {

  fun publish(
    entry: ActivityEntry,
    newState: LockState,
    code: String?,
    system: Boolean
  ) {
    bus.publish(LockInfoEvent.Modify.from(entry, newState, code, system))
  }
}
