/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock

import android.app.Activity
import android.app.Application
import android.app.Service
import android.app.job.JobService
import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.service.PadLockJobService
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.settings.ClearAllEvent
import com.pyamsoft.padlock.settings.ClearDatabaseEvent
import com.pyamsoft.padlock.settings.SwitchLockTypeEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.cache.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
object PadLockProvider {

  private val clearAllBus = RxBus.create<ClearAllEvent>()
  private val clearDatabaseBus = RxBus.create<ClearDatabaseEvent>()
  private val recreateBus = RxBus.create<Unit>()
  private val settingsStateBus = RxBus.create<SwitchLockTypeEvent>()

  @JvmStatic
  @Provides
  internal fun provideClearAllBus(): EventBus<ClearAllEvent> = clearAllBus

  @JvmStatic
  @Provides
  internal fun provideClearDatabaseBus(): EventBus<ClearDatabaseEvent> = clearDatabaseBus

  @JvmStatic
  @Provides
  fun provideLockTypeBus(): EventBus<SwitchLockTypeEvent> = settingsStateBus

  @JvmStatic
  @Provides
  @Named("recreate_publisher")
  fun provideRecreatePublisher(): Publisher<Unit> = recreateBus

  @JvmStatic
  @Provides
  @Named("recreate_listener")
  fun provideRecreateListener(): Listener<Unit> = recreateBus

  @JvmStatic
  @Provides
  fun provideContext(application: Application): Context = application

  @JvmStatic
  @Provides
  @Named("cache_list_state")
  fun provideListStateCache(): Cache = ListStateUtil

  @JvmStatic
  @Provides
  fun provideMainActivityClass(): Class<out Activity> = MainActivity::class.java

  @JvmStatic
  @Provides
  fun provideServiceClass(): Class<out Service> = PadLockService::class.java

  @JvmStatic
  @Provides
  fun provideJobServiceClass(): Class<out JobService> = PadLockJobService::class.java

  @JvmStatic
  @Provides
  @Named("notification_icon")
  @DrawableRes
  fun provideNotificationIcon(): Int = R.drawable.ic_padlock_notification

  @JvmStatic
  @Provides
  @Named("notification_color")
  @ColorRes
  fun provideNotificationColor(): Int = R.color.blue500
}
