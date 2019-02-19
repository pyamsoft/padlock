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

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.pydroid.core.cache.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class PurgeSingletonModule {

  @Binds
  internal abstract fun providePurgeInteractor(impl: PurgeInteractorImpl): PurgeInteractor

  @Binds
  @Named("cache_purge")
  internal abstract fun providePurgeCache(impl: PurgeInteractorImpl): Cache

  @Binds
  @Named("interactor_purge")
  internal abstract fun providePurgeInteractorDb(impl: PurgeInteractorDb): PurgeInteractor
}
