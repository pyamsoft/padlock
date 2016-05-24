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

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import com.pyamsoft.padlock.app.service.LockServicePresenter;
import dagger.Module;
import dagger.Provides;

@Module public class LockServiceModule {

  @Provides LockServicePresenter provideLockServicePresenter(
      final LockServicePresenterImpl presenter) {
    return presenter;
  }

  @Provides LockServiceInteractor provideLockServiceInteractor(
      final LockServiceInteractorImpl interactor) {
    return interactor;
  }

  @Provides KeyguardManager provideKeyguardManager(final Context context) {
    return (KeyguardManager) context.getApplicationContext()
        .getSystemService(Context.KEYGUARD_SERVICE);
  }

  @Provides PackageManager providePackageManager(final Context context) {
    return context.getApplicationContext().getPackageManager();
  }
}
