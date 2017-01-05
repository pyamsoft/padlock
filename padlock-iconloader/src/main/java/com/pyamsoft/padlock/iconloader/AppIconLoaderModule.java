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

package com.pyamsoft.padlock.iconloader;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.pydroid.rx.scopes.ActivityScope;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module class AppIconLoaderModule {

  @ActivityScope @Provides AppIconLoaderPresenter provideAppIconLoaderPresenter(
      final AppIconLoaderInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    return new AppIconLoaderPresenterImpl(interactor, obsScheduler, subScheduler);
  }

  @ActivityScope @Provides AppIconLoaderInteractor provideAppIconLoaderInteractor(
      @NonNull PackageManagerWrapper packageManagerWrapper) {
    return new AppIconLoaderInteractorImpl(packageManagerWrapper);
  }
}
