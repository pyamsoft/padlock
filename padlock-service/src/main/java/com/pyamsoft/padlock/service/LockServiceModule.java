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

package com.pyamsoft.padlock.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class LockServiceModule {

  @Provides LockServicePresenter provideLockServicePresenter(
      @NonNull LockServiceInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    return new LockServicePresenter(interactor, obsScheduler, subScheduler);
  }

  @Provides LockServiceInteractor provideLockServiceInteractor(@NonNull Context context,
      @NonNull PadLockPreferences preference, @NonNull JobSchedulerCompat jobManager,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockDB padLockDB,
      @Named("lockscreen") Class<? extends Activity> lockScreenActivityClass,
      @Named("recheck") Class<? extends IntentService> recheckServiceClass,
      @NonNull LockServiceStateInteractor stateInteractor) {
    return new LockServiceInteractor(context.getApplicationContext(), preference, jobManager,
        packageManagerWrapper, padLockDB, lockScreenActivityClass, recheckServiceClass,
        stateInteractor);
  }
}
