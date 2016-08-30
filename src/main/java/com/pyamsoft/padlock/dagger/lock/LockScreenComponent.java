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

import com.pyamsoft.padlock.app.lock.LockScreenPresenterLoader;
import com.pyamsoft.padlock.dagger.iconloader.AppIconLoaderModule;
import com.pyamsoft.pydroid.base.app.ActivityScope;
import dagger.Subcomponent;

@ActivityScope @Subcomponent(modules = {
    LockScreenModule.class, MasterPinModule.class, AppIconLoaderModule.class
}) public interface LockScreenComponent {

  void inject(LockScreenPresenterLoader loader);
}
