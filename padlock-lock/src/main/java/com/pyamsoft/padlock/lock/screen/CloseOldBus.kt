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

package com.pyamsoft.padlock.lock.screen

import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CloseOldBus @Inject internal constructor() : EventBus<CloseOldEvent> {

  private val bus: EventBus<CloseOldEvent> = RxBus.create()

  override fun listen(): Observable<CloseOldEvent> = bus.listen()

  override fun publish(event: CloseOldEvent) {
    bus.publish(event)
  }
}
