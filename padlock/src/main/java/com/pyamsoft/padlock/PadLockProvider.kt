/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock

import android.app.Activity
import android.app.Application
import android.app.IntentService
import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderModule
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class PadLockProvider(
  private val application: Application,
  private val loaderModule: LoaderModule,
  private val mainActivityClass: Class<out Activity>,
  private val recheckServiceClass: Class<out IntentService>
) {

  @Provides
  @CheckResult
  fun provideApplication(): Application = application

  @Provides
  @CheckResult
  fun provideContext(): Context = provideApplication()

  @Provides
  @CheckResult
  fun provideImageLoader(): ImageLoader = loaderModule.provideImageLoader()

  @Provides
  @CheckResult
  @Named("cache_image_loader")
  fun provideImageLoaderCache(): Cache = loaderModule.provideImageLoaderCache()

  @Provides
  @CheckResult
  @Named("cache_list_state")
  fun provideListStateCache(): Cache = ListStateUtil

  @Provides
  @CheckResult
  @Named("main_activity")
  fun provideMainActivityClass(): Class<out Activity> = mainActivityClass

  @Provides
  @CheckResult
  @Named("recheck")
  fun provideRecheckServiceClass(): Class<out IntentService> = recheckServiceClass
}
