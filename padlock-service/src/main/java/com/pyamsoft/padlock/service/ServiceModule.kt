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

package com.pyamsoft.padlock.service

import android.app.Activity
import android.app.IntentService
import android.content.Context
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.lock.master.MasterPinInteractor
import com.pyamsoft.pydroid.bus.EventBus
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module class ServiceModule {

  @Singleton @Provides internal fun provideServiceInteractor(
      context: Context, preferences: LockScreenPreferences, jobSchedulerCompat: JobSchedulerCompat,
      packageActivityManager: PackageActivityManager, padLockDBQuery: PadLockDBQuery,
      @Named("lockscreen") lockScreenActivityClass: Class<out Activity>,
      @Named("recheck") recheckServiceClass: Class<out IntentService>,
      stateInteractor: LockServiceStateInteractor): LockServiceInteractor {
    return LockServiceInteractorImpl(context, preferences, jobSchedulerCompat,
        packageActivityManager, padLockDBQuery, lockScreenActivityClass, recheckServiceClass,
        stateInteractor)
  }

  @Singleton @Provides internal fun provideServiceStateInteractor(
      masterPinInteractor: MasterPinInteractor): LockServiceStateInteractor {
    return LockServiceStateInteractorImpl(masterPinInteractor)
  }

  @Singleton @Provides internal fun provideLockPassBus(): EventBus<LockPassEvent> {
    return LockPassBus()
  }

  @Singleton @Provides internal fun provideRecheckBus(): EventBus<RecheckEvent> {
    return RecheckEventBus()
  }

  @Singleton @Provides internal fun provideServiceFinishBus(): EventBus<ServiceFinishEvent> {
    return ServiceFinishBus()
  }
}

