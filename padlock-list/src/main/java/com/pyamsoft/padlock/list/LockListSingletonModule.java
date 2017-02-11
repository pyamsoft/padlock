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

package com.pyamsoft.padlock.list;

import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module public class LockListSingletonModule {

  @Singleton @Provides LockListInteractor provideLockScreenInteractor(PadLockDB padLockDB,
      PackageManagerWrapper packageManagerWrapper, PadLockPreferences preferences) {
    return new LockListInteractor(padLockDB, packageManagerWrapper, preferences);
  }
}
