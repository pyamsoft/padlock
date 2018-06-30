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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.model.purge.PurgeAllEvent
import com.pyamsoft.padlock.model.purge.PurgeEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurgePublisher @Inject internal constructor(
  private val bus: EventBus<PurgeEvent>,
  private val allBus: EventBus<PurgeAllEvent>
) {

  fun publish(event: PurgeEvent) {
    bus.publish(event)
  }

  fun publish(event: PurgeAllEvent) {
    allBus.publish(event)
  }
}
