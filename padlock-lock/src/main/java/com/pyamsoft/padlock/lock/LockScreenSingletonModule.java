/*
 * Copyright 2016 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.lock;

import android.app.IntentService;
import android.content.Context;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.lock.master.MasterPinInteractor;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;

@Module public class LockScreenSingletonModule {

  @Singleton @Provides LockScreenInteractor provideLockScreenInteractor(
      PadLockPreferences preference, PackageManagerWrapper packageManagerWrapper) {
    return new LockScreenInteractor(preference, packageManagerWrapper);
  }

  @Singleton @Provides LockScreenEntryInteractor provideLockScreenEntryInteractor(Context context,
      PadLockPreferences preference, JobSchedulerCompat jobSchedulerCompat,
      MasterPinInteractor masterPinInteractor, PadLockDB padLockDB,
      @Named("recheck") Class<? extends IntentService> recheckServiceClass) {
    return new LockScreenEntryInteractor(context.getApplicationContext(), preference,
        jobSchedulerCompat, masterPinInteractor, padLockDB, recheckServiceClass);
  }
}