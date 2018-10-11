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
import android.app.Service
import android.app.job.JobService
import android.content.Context
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.ModuleProvider
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class PadLockProvider(
  private val application: Application,
  moduleProvider: ModuleProvider,
  private val mainActivityClass: Class<out Activity>,
  private val serviceClass: Class<out Service>,
  private val jobServiceClass: Class<out JobService>
) {

  private val enforcer = moduleProvider.enforcer()
  private val imageLoader = moduleProvider
      .loaderModule()
      .provideImageLoader()

  @Provides
  @CheckResult
  fun provideEnforcer(): Enforcer = enforcer

  @Provides
  @CheckResult
  fun provideApplication(): Application = application

  @Provides
  @CheckResult
  fun provideContext(): Context = provideApplication()

  @Provides
  @CheckResult
  fun provideImageLoader(): ImageLoader = imageLoader

  @Provides
  @CheckResult
  @Named("cache_list_state")
  fun provideListStateCache(): Cache = ListStateUtil

  @Provides
  @CheckResult
  fun provideMainActivityClass(): Class<out Activity> = mainActivityClass

  @Provides
  @CheckResult
  fun provideServiceClass(): Class<out Service> = serviceClass

  @Provides
  @CheckResult
  fun provideJobServiceClass(): Class<out JobService> = jobServiceClass

  @Provides
  @CheckResult
  @Named("notification_icon")
  @DrawableRes
  fun provideNotificationIcon(): Int = R.drawable.ic_padlock_notification

  @Provides
  @CheckResult
  @Named("notification_color")
  @ColorRes
  fun provideNotificationColor(): Int = R.color.blue500
}
