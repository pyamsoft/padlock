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
import com.pyamsoft.padlock.app.list.AppEntryAdapterPresenter;
import com.pyamsoft.padlock.app.list.LockListPresenter;
import com.pyamsoft.padlock.dagger.ActivityScope;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.AppEntry;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class LockListModule {

  @ActivityScope @Provides LockListPresenter provideLockScreenPresenter(
      @NonNull LockListInteractor interactor, @NonNull LockServiceStateInteractor stateInteractor,
      @Named("main") Scheduler mainScheduler, @Named("io") Scheduler ioScheduler) {
    return new LockListPresenterImpl(interactor, stateInteractor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides LockListInteractor provideLockScreenInteractor(
      final LockListInteractorImpl interactor) {
    return interactor;
  }

  @ActivityScope @Provides AppEntryAdapterPresenter provideAppEntryAdapterPresenter(
      final AppEntryAdapterPresenterImpl adapter) {
    return adapter;
  }

  @ActivityScope @Provides AdapterInteractor<AppEntry> provideAppEntryAdapterInteractor(
      final PackageManagerWrapper packageManagerWrapper) {
    return new AdapterInteractorImpl<>(packageManagerWrapper);
  }
}
