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

package com.pyamsoft.padlock.dagger.lock;

import com.pyamsoft.padlock.app.lock.LockInteractor;
import com.pyamsoft.padlock.app.lock.LockPresenter;
import dagger.Module;
import dagger.Provides;

@Module public class LockModule {

  @Provides LockPresenter provideLockPresenter(final LockPresenterImpl presenter) {
    return presenter;
  }

  @Provides LockInteractor provideLockInteractor(final LockInteractorImpl interactor) {
    return interactor;
  }
}
