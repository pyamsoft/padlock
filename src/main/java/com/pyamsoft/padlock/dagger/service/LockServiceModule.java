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

package com.pyamsoft.padlock.dagger.service;

import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.service.LockServicePresenter;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.PadLockDB;
import com.pyamsoft.padlock.dagger.wrapper.JobSchedulerCompat;
import com.pyamsoft.pydroid.ActivityScope;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class LockServiceModule {

  @ActivityScope @Provides LockServicePresenter provideLockServicePresenter(
      LockServiceInteractor interactor, LockServiceStateInteractor stateInteractor,
      @Named("main") Scheduler mainScheduler, @Named("io") Scheduler ioScheduler) {
    return new LockServicePresenterImpl(stateInteractor, interactor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides LockServiceInteractor provideLockServiceInteractor(
      PadLockPreferences preference, JobSchedulerCompat jobManager,
      PackageManagerWrapper packageManagerWrapper, PadLockDB padLockDB) {
    return new LockServiceInteractorImpl(preference, jobManager, packageManagerWrapper, padLockDB);
  }
}
