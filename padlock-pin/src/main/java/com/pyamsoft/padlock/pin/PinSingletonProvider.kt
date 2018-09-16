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

package com.pyamsoft.padlock.pin

import com.pyamsoft.padlock.model.pin.CheckPinEvent
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Module
import dagger.Provides

@Module
object PinSingletonProvider {

  private val clearBus = RxBus.create<ClearPinEvent>()
  private val createBus = RxBus.create<CreatePinEvent>()
  private val checkBus = RxBus.create<CheckPinEvent>()

  @JvmStatic
  @Provides
  internal fun provideClearPublisher(): Publisher<ClearPinEvent> = clearBus

  @JvmStatic
  @Provides
  internal fun provideClearListener(): Listener<ClearPinEvent> = clearBus

  @JvmStatic
  @Provides
  internal fun provideCreatePublisher(): Publisher<CreatePinEvent> = createBus

  @JvmStatic
  @Provides
  internal fun provideCreateListener(): Listener<CreatePinEvent> = createBus

  @JvmStatic
  @Provides
  internal fun provideCheckBus(): EventBus<CheckPinEvent> = checkBus

  @JvmStatic
  @Provides
  internal fun provideCheckListener(): Listener<CheckPinEvent> = checkBus
}
