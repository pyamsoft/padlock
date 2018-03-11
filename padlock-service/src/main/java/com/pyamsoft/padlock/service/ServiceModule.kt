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

import com.pyamsoft.padlock.api.DeviceLockStateProvider
import com.pyamsoft.padlock.api.LockServiceInteractor
import com.pyamsoft.padlock.api.LockServiceStateInteractor
import com.pyamsoft.padlock.api.UsageEventProvider
import com.pyamsoft.padlock.model.RecheckEvent
import com.pyamsoft.padlock.model.ServiceFinishEvent
import com.pyamsoft.padlock.service.device.DeviceLockStateProviderImpl
import com.pyamsoft.padlock.service.device.UsageEventProviderImpl
import com.pyamsoft.pydroid.bus.EventBus
import dagger.Binds
import dagger.Module

@Module
abstract class ServiceModule {

  @Binds
  internal abstract fun provideServiceInteractor(
    impl: LockServiceInteractorImpl
  ): LockServiceInteractor

  @Binds
  internal abstract fun provideServiceStateInteractor(
    impl: LockServiceStateInteractorImpl
  ): LockServiceStateInteractor

  @Binds
  internal abstract fun provideRecheckBus(bus: RecheckEventBus): EventBus<RecheckEvent>

  @Binds
  internal abstract fun serviceFinishBus(bus: ServiceFinishBus): EventBus<ServiceFinishEvent>

  @Binds
  internal abstract fun deviceLockState(impl: DeviceLockStateProviderImpl): DeviceLockStateProvider

  @Binds
  internal abstract fun usageEvents(impl: UsageEventProviderImpl): UsageEventProvider
}
