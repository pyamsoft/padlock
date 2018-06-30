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

import com.pyamsoft.padlock.api.PinEntryInteractor
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import dagger.Binds
import dagger.Module

@Module
abstract class PinModule {

  @Binds
  internal abstract fun provideClearBus(bus: ClearPinBus): EventBus<ClearPinEvent>

  @Binds
  internal abstract fun provideCreateBus(bus: CreatePinBus): EventBus<CreatePinEvent>

  @Binds
  internal abstract fun provideInteractor(impl: PinEntryInteractorImpl): PinEntryInteractor
}
