/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.purge

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module class PurgeModule {

  @Singleton @Provides @CheckResult internal fun providePurgeBus(): EventBus<PurgeEvent> {
    return PurgeBus()
  }

  @Singleton @Provides @CheckResult internal fun providePurgeAllBus(): EventBus<PurgeAllEvent> {
    return PurgeAllBus()
  }

  @Singleton @Provides @CheckResult internal fun providePurgeInteractorCache(
      applicationManager: PackageApplicationManager, deleteDb: PadLockDBDelete,
      queryDb: PadLockDBQuery): PurgeInteractorCache {
    return PurgeInteractorCache(PurgeInteractorImpl(applicationManager, deleteDb, queryDb))
  }

  @Singleton @Provides @CheckResult internal fun providePurgeInteractor(
      cache: PurgeInteractorCache): PurgeInteractor {
    return cache
  }

  @Singleton @Provides @CheckResult @Named("cache_purge") internal fun provideCache(
      cache: PurgeInteractorCache): Cache {
    return cache
  }
}

