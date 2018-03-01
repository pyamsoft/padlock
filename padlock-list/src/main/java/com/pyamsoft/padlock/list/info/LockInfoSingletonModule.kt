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

import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockInfoUpdater
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.cache.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class LockInfoSingletonModule {

  @Binds
  internal abstract fun provideBus(bus: LockInfoBus): EventBus<LockInfoEvent>

  @Binds
  internal abstract fun provideChangeBus(
      bus: LockInfoChangeBus
  ): EventBus<LockInfoEvent.Callback>

  @Binds
  internal abstract fun provideInteractorCache(impl: LockInfoInteractorCache): LockInfoInteractor

  @Binds
  @Named("interactor_lock_info")
  internal abstract fun provideInteractor(impl: LockInfoInteractorImpl): LockInfoInteractor

  @Binds
  @Named("cache_lock_info")
  internal abstract fun provideCache(cache: LockInfoInteractorCache): Cache

  @Binds
  internal abstract fun provideUpdater(cache: LockInfoInteractorCache): LockInfoUpdater
}
