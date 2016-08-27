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

package com.pyamsoft.padlock.dagger.base;

import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.list.LockInfoPresenter;
import com.pyamsoft.padlock.app.lock.LockScreen;
import com.pyamsoft.padlock.app.lock.PinScreen;
import com.pyamsoft.padlock.dagger.ActivityScope;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class AppIconLoaderModule {

  @ActivityScope @Provides
  AppIconLoaderPresenter<LockScreen> provideLockScreenAppIconLoaderPresenter(
      final AppIconLoaderInteractor interactor, @Named("main") Scheduler mainScheduler,
      @Named("io") Scheduler ioScheduler) {
    return new AppIconLoaderPresenterImpl<>(interactor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides
  AppIconLoaderPresenter<LockInfoPresenter.LockInfoView> provideLockInfoViewAppIconLoaderPresenter(
      final AppIconLoaderInteractor interactor, @Named("main") Scheduler mainScheduler,
      @Named("io") Scheduler ioScheduler) {
    return new AppIconLoaderPresenterImpl<>(interactor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides AppIconLoaderPresenter<PinScreen> providePinScreenAppIconLoaderPresenter(
      final AppIconLoaderInteractor interactor, @Named("main") Scheduler mainScheduler,
      @Named("io") Scheduler ioScheduler) {
    return new AppIconLoaderPresenterImpl<>(interactor, mainScheduler, ioScheduler);
  }

  @ActivityScope @Provides AppIconLoaderInteractor provideAppIconLoaderInteractor(
      final AppIconLoaderInteractorImpl interactor) {
    return interactor;
  }
}
