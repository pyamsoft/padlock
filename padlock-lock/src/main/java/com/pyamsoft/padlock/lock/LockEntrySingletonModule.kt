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

package com.pyamsoft.padlock.lock

import android.app.IntentService
import android.content.Context
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBInsert
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.lock.helper.LockHelper
import com.pyamsoft.padlock.lock.master.MasterPinInteractor
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module class LockEntrySingletonModule {

  @Singleton @Provides @CheckResult internal fun provideInteractor(context: Context,
      lockHelper: LockHelper, lockScreenPreferences: LockScreenPreferences,
      jobSchedulerCompat: JobSchedulerCompat, masterPinInteractor: MasterPinInteractor,
      dbInsert: PadLockDBInsert, dbUpdate: PadLockDBUpdate,
      @Named("recheck") recheckServiceClass: Class<out IntentService>): LockEntryInteractor {
    return LockEntryInteractorImpl(context, lockHelper, lockScreenPreferences,
        jobSchedulerCompat, masterPinInteractor, dbInsert, dbUpdate, recheckServiceClass)
  }
}

