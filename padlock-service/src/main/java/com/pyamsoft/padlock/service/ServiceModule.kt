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

import com.pyamsoft.padlock.api.service.DeviceLockStateProvider
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.UsageEventProvider
import com.pyamsoft.padlock.service.device.DeviceLockStateProviderImpl
import com.pyamsoft.padlock.service.device.UsageEventProviderImpl
import dagger.Binds
import dagger.Module

@Module
abstract class ServiceModule {

  @Binds
  internal abstract fun provideServiceInteractor(
    impl: LockServiceInteractorImpl
  ): LockServiceInteractor

  @Binds
  internal abstract fun deviceLockState(impl: DeviceLockStateProviderImpl): DeviceLockStateProvider

  @Binds
  internal abstract fun usageEvents(impl: UsageEventProviderImpl): UsageEventProvider
}
