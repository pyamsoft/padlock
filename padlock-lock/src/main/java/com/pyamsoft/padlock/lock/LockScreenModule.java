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
import com.pyamsoft.padlock.pin.MasterPinInteractor;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module class LockScreenModule {

  @Provides LockScreenPresenter provideLockScreenPresenter(LockScreenInteractor interactor,
      @Named("obs") Scheduler obsScheduler, @Named("sub") Scheduler subScheduler) {
    return new LockScreenPresenterImpl(interactor, obsScheduler, subScheduler);
  }

  @Provides LockScreenInteractor provideLockScreenInteractor(Context context,
      PadLockPreferences preference, JobSchedulerCompat jobSchedulerCompat,
      MasterPinInteractor masterPinInteractor, PackageManagerWrapper packageManagerWrapper,
      PadLockDB padLockDB, @Named("recheck") Class<? extends IntentService> recheckServiceClass) {
    return new LockScreenInteractorImpl(context, preference, jobSchedulerCompat,
        masterPinInteractor, packageManagerWrapper, padLockDB, recheckServiceClass);
  }
}