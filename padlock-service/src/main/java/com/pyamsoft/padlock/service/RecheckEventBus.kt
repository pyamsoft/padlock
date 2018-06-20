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

package com.pyamsoft.padlock.service

import com.pyamsoft.padlock.model.RecheckEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RecheckEventBus @Inject internal constructor() : EventBus<RecheckEvent> {

  private val bus: EventBus<RecheckEvent> = RxBus.create()

  override fun listen(): Observable<RecheckEvent> = bus.listen()

  override fun publish(event: RecheckEvent) {
    bus.publish(event)
  }
}
