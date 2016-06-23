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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import com.pyamsoft.padlock.app.list.AdapterPresenter;
import com.pyamsoft.padlock.app.list.LockInfoAdapter;
import com.pyamsoft.padlock.app.list.LockInfoPresenter;
import com.pyamsoft.padlock.dagger.ActivityScope;
import com.pyamsoft.padlock.model.ActivityEntry;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class LockInfoModule {

  @ActivityScope @Provides LockInfoPresenter provideLockInfoPresenter(
      LockInfoInteractor infoInteractor, @Named("main") Scheduler mainScheduler,
      @Named("io") Scheduler ioScheduler) {
    return new LockInfoPresenter(infoInteractor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides LockInfoInteractor provideLockInfoInteractor(
      final LockInfoInteractorImpl interactor) {
    return interactor;
  }

  @ActivityScope @Provides
  AdapterPresenter<ActivityEntry, LockInfoAdapter.ViewHolder> provideActivityEntryAdapterPresenter(
      final AdapterInteractor<ActivityEntry> interactor, @Named("main") Scheduler mainScheduler,
      @Named("io") Scheduler ioScheduler) {
    return new ActivityEntryAdapterPresenter(interactor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides AdapterInteractor<ActivityEntry> provideActivityEntryAdapterInteractor(
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    return new AdapterInteractorImpl<>(packageManagerWrapper);
  }
}
