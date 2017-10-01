/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
      masterPinInteractor: MasterPinInteractor): LockServiceStateInteractor =
      LockServiceStateInteractorImpl(masterPinInteractor)

  @Singleton @Provides internal fun provideRecheckBus(): EventBus<RecheckEvent> = RecheckEventBus()

  @Singleton @Provides internal fun provideServiceFinishBus(): EventBus<ServiceFinishEvent> =
      ServiceFinishBus()
}

