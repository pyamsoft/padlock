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

package com.pyamsoft.padlock.lock

import android.app.IntentService
import android.content.Context
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBInsert
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.lock.helper.LockHelper
import com.pyamsoft.padlock.lock.master.MasterPinInteractor
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class LockEntrySingletonModule {

  @Singleton
  @Provides
  @CheckResult internal fun provideInteractorCache(context: Context,
      lockHelper: LockHelper, lockScreenPreferences: LockScreenPreferences,
      jobSchedulerCompat: JobSchedulerCompat, masterPinInteractor: MasterPinInteractor,
      dbInsert: PadLockDBInsert, dbUpdate: PadLockDBUpdate, dbQuery: PadLockDBQuery,
      @Named(
          "recheck") recheckServiceClass: Class<out IntentService>): LockEntryInteractorImplCache {
    return LockEntryInteractorImplCache(
        LockEntryInteractorImpl(context, lockHelper, lockScreenPreferences,
            jobSchedulerCompat, masterPinInteractor, dbInsert, dbUpdate, dbQuery,
            recheckServiceClass))
  }

  @Singleton
  @Provides
  @CheckResult internal fun provideInteractor(
      cache: LockEntryInteractorImplCache): LockEntryInteractor = cache

  @Singleton
  @Provides
  @Named("cache_lock_entry")
  @CheckResult internal fun provideCache(cache: LockEntryInteractorImplCache): Cache = cache

  @Singleton
  @Provides internal fun provideLockPassBus(): EventBus<LockPassEvent> = LockPassBus()
}

