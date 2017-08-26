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

package com.pyamsoft.padlock.list.info

import android.app.Activity
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.list.modify.LockStateModifyInteractor
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class LockInfoSingletonModule() {

  @Singleton
  @Provides
  @CheckResult internal fun provideBus(): EventBus<LockInfoEvent> =
      LockInfoBus()

  @Singleton
  @Provides
  @CheckResult internal fun provideInteractorCache(queryDb: PadLockDBQuery,
      packageActivityManager: PackageActivityManager, preferences: OnboardingPreferences,
      updateDb: PadLockDBUpdate, lockStateModifyInteractor: LockStateModifyInteractor,
      @Named("lockscreen") lockScreenClass: Class<out Activity>): LockInfoInteractorCache {
    return LockInfoInteractorCache(
        LockInfoInteractorImpl(queryDb, packageActivityManager, preferences, updateDb,
            lockStateModifyInteractor, lockScreenClass))
  }

  @Singleton
  @Provides
  @CheckResult internal fun provideInteractor(
      cache: LockInfoInteractorCache): LockInfoInteractor = cache

  @Singleton
  @Provides
  @CheckResult
  @Named("cache_lock_info") internal fun provideCache(
      cache: LockInfoInteractorCache): Cache = cache
}

