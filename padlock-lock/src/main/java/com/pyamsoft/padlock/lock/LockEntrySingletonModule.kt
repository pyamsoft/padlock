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

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.api.LockEntryInteractor
import com.pyamsoft.padlock.api.LockPassed
import com.pyamsoft.padlock.lock.passed.LockPassedImpl
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class LockEntrySingletonModule {

  @Binds
  internal abstract fun provideInteractorCache(
      impl: LockEntryInteractorCache
  ): LockEntryInteractor

  @Binds
  @Named("interactor_lock_entry")
  internal abstract fun provideInteractor(impl: LockEntryInteractorImpl): LockEntryInteractor

  @Binds
  @Named("cache_lock_entry")
  internal abstract fun provideCache(impl: LockEntryInteractorCache): Cache

  @Binds
  internal abstract fun provideForegroundBus(bus: ForegroundEventBus): EventBus<ForegroundEvent>

  @Binds
  internal abstract fun provideLockPassed(impl: LockPassedImpl): LockPassed
}
