/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.base.wrapper

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module class PackageManagerWrapperModule {

  @Singleton @Provides internal fun providePackageManagerWrapper(
      context: Context): PackageManagerWrapperImpl {
    return PackageManagerWrapperImpl(context)
  }

  @Singleton @Provides internal fun providePackageActivityManager(
      manager: PackageManagerWrapperImpl): PackageActivityManager {
    return manager
  }

  @Singleton @Provides internal fun providePackageLabelManager(
      manager: PackageManagerWrapperImpl): PackageLabelManager {
    return manager
  }

  @Singleton @Provides internal fun providePackageApplicationManager(
      manager: PackageManagerWrapperImpl): PackageApplicationManager {
    return manager
  }

  @Singleton @Provides internal fun providePackageDrawableManager(
      manager: PackageManagerWrapperImpl): PackageDrawableManager {
    return manager
  }
}
