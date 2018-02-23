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
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.pydroid.ApplicationModule
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderModule
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class PadLockProvider(
    private val pyDroidModule: ApplicationModule,
    private val loaderModule: LoaderModule,
    private val mainActivityClass: Class<out Activity>,
    private val recheckServiceClass: Class<out IntentService>
) : ApplicationModule, LoaderModule {

  @Provides
  override fun provideApplication(): Application = pyDroidModule.provideApplication()

  @Provides
  override fun provideContext(): Context = pyDroidModule.provideContext()

  @Provides
  @Named("computation")
  override fun provideComputationScheduler(): Scheduler = pyDroidModule.provideComputationScheduler()

  @Provides
  @Named("io")
  override fun provideIoScheduler(): Scheduler = pyDroidModule.provideIoScheduler()

  @Provides
  @Named("main")
  override fun provideMainThreadScheduler(): Scheduler = pyDroidModule.provideMainThreadScheduler()

  @Provides
  override fun provideImageLoader(): ImageLoader = loaderModule.provideImageLoader()

  @Provides
  override fun provideImageLoaderCache(): Cache = loaderModule.provideImageLoaderCache()

  @Provides
  @Named("cache_list_state")
  fun provideListStateCache(): Cache = ListStateUtil

  @Provides
  @Named("main_activity")
  fun provideMainActivityClass(): Class<out Activity> = mainActivityClass

  @Provides
  @Named("recheck")
  fun provideRecheckServiceClass(): Class<out IntentService> = recheckServiceClass
}
