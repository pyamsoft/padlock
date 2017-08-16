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

package com.pyamsoft.padlock.list

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager
import com.pyamsoft.padlock.base.wrapper.PackageLabelManager
import com.pyamsoft.padlock.list.modify.LockStateModifyInteractor
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module class LockListSingletonModule() {

  @Singleton @Provides @CheckResult internal fun provideInteractorCache(queryDb: PadLockDBQuery,
      applicationManager: PackageApplicationManager, labelManager: PackageLabelManager,
      activityManager: PackageActivityManager, onboardingPreferences: OnboardingPreferences,
      lockListPreferences: LockListPreferences): LockListInteractorCache {
    return LockListInteractorCache(LockListInteractorImpl(queryDb, applicationManager, labelManager,
        activityManager, onboardingPreferences, lockListPreferences))
  }

  @Singleton @Provides @CheckResult internal fun provideInteractor(
      cache: LockListInteractorCache): LockListInteractor {
    return cache
  }

  @Singleton @Provides @CheckResult internal fun provideItemInteractor(
      @Named("cache_lock_list") cache: Cache,
      modifyInteractor: LockStateModifyInteractor): LockListItemInteractor {
    return LockListItemInteractorImpl(cache, modifyInteractor)
  }

  @Singleton @Provides @CheckResult @Named("cache_lock_list") internal fun provideCache(
      cache: LockListInteractorCache): Cache {
    return cache
  }
}

