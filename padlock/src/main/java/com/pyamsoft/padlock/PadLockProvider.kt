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
import android.app.IntentService
import android.content.Context
import com.pyamsoft.pydroid.PYDroidModule
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderModule
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class PadLockProvider(
    private val pyDroidModule: PYDroidModule<PadLock>,
    private val loaderModule: LoaderModule,
    private val mainActivityClass: Class<out Activity>,
    private val recheckServiceClass: Class<out IntentService>
) {

  @Provides
  internal fun provideApplication(): PadLock = pyDroidModule.provideApplication()

  @Provides
  internal fun provideContext(): Context = provideApplication()

  @Provides
  @Named("main_activity")
  internal fun provideMainActivityClass(): Class<out Activity> = mainActivityClass

  @Provides
  @Named("recheck")
  internal fun provideRecheckServiceClass(): Class<out IntentService> = recheckServiceClass

  @Provides
  @Named("computation")
  internal fun provideComputationScheduler(): Scheduler = pyDroidModule.provideComputationScheduler()

  @Provides
  @Named("io")
  internal fun provideIOScheduler(): Scheduler = pyDroidModule.provideIoScheduler()

  @Provides
  @Named("main")
  internal fun provideMainScheduler(): Scheduler = pyDroidModule.provideMainThreadScheduler()

  @Provides
  internal fun provideImageLoader(): ImageLoader = loaderModule.provideImageLoader()
}
