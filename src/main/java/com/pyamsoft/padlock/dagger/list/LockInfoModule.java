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
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.list.LockInfoPresenter;
import com.pyamsoft.padlock.dagger.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.PadLockDB;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class LockInfoModule {

  @Provides LockInfoPresenter provideLockInfoPresenter(
      @NonNull AppIconLoaderPresenter<LockInfoPresenter.LockInfoView> iconLoader,
      LockInfoInteractor infoInteractor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    return new LockInfoPresenterImpl(iconLoader, infoInteractor, obsScheduler, subScheduler);
  }

  @Provides LockInfoInteractor provideLockInfoInteractor(PadLockDB padLockDB,
      PackageManagerWrapper packageManagerWrapper, @NonNull PadLockPreferences preferences) {
    return new LockInfoInteractorImpl(padLockDB, packageManagerWrapper, preferences);
  }
}
