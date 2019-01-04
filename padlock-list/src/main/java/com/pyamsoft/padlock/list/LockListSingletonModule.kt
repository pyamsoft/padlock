/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.cache.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class LockListSingletonModule {

  @Binds
  internal abstract fun provideInteractor(impl: LockListInteractorImpl): LockListInteractor

  @Binds
  @Named("cache_lock_list")
  internal abstract fun provideCache(impl: LockListInteractorImpl): Cache

  @Binds
  @Named("interactor_lock_list")
  internal abstract fun provideInteractorDb(impl: LockListInteractorDb): LockListInteractor
}
