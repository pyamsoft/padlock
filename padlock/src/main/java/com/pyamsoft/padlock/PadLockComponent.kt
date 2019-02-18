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
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.pyamsoft.padlock.PadLockComponent.PadLockModule
import com.pyamsoft.padlock.PadLockComponent.PadLockProvider
import com.pyamsoft.padlock.R.color
import com.pyamsoft.padlock.R.drawable
import com.pyamsoft.padlock.base.BaseModule
import com.pyamsoft.padlock.base.BaseProvider
import com.pyamsoft.padlock.base.database.DatabaseProvider
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.list.LockInfoComponent
import com.pyamsoft.padlock.list.LockInfoExplainComponent
import com.pyamsoft.padlock.list.LockInfoItemComponent
import com.pyamsoft.padlock.list.LockListComponent
import com.pyamsoft.padlock.list.LockListItemComponent
import com.pyamsoft.padlock.list.LockListSingletonModule
import com.pyamsoft.padlock.list.LockListSingletonProvider
import com.pyamsoft.padlock.list.info.LockInfoSingletonModule
import com.pyamsoft.padlock.list.info.LockInfoSingletonProvider
import com.pyamsoft.padlock.list.modify.LockStateModule
import com.pyamsoft.padlock.lock.LockScreenComponent
import com.pyamsoft.padlock.lock.LockSingletonModule
import com.pyamsoft.padlock.lock.LockSingletonProvider
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.main.MainComponent
import com.pyamsoft.padlock.main.MainFragmentComponent
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.pin.ClearPinPresenter
import com.pyamsoft.padlock.pin.ClearPinPresenterImpl
import com.pyamsoft.padlock.pin.PinBaseFragment
import com.pyamsoft.padlock.pin.PinComponent
import com.pyamsoft.padlock.pin.PinSingletonModule
import com.pyamsoft.padlock.pin.PinSingletonProvider
import com.pyamsoft.padlock.purge.PurgeAllDialog
import com.pyamsoft.padlock.purge.PurgeComponent
import com.pyamsoft.padlock.purge.PurgeItemComponent
import com.pyamsoft.padlock.purge.PurgeSingleItemDialog
import com.pyamsoft.padlock.purge.PurgeSingletonModule
import com.pyamsoft.padlock.purge.PurgeSingletonProvider
import com.pyamsoft.padlock.receiver.BootReceiver
import com.pyamsoft.padlock.service.PadLockJobService
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.service.PauseComponent
import com.pyamsoft.padlock.service.ServiceSingletonModule
import com.pyamsoft.padlock.service.ServiceSingletonProvider
import com.pyamsoft.padlock.settings.ClearAllEvent
import com.pyamsoft.padlock.settings.ClearAllPresenter
import com.pyamsoft.padlock.settings.ClearAllPresenterImpl
import com.pyamsoft.padlock.settings.ClearDatabaseEvent
import com.pyamsoft.padlock.settings.ClearDatabasePresenter
import com.pyamsoft.padlock.settings.ClearDatabasePresenterImpl
import com.pyamsoft.padlock.settings.ConfirmDeleteAllDialog
import com.pyamsoft.padlock.settings.ConfirmDeleteDatabaseDialog
import com.pyamsoft.padlock.settings.SettingsComponent
import com.pyamsoft.padlock.settings.SettingsSingletonModule
import com.pyamsoft.padlock.settings.SwitchLockTypeEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
      PadLockProvider::class, PadLockModule::class, BaseModule::class, BaseProvider::class,
      DatabaseProvider::class, PinSingletonModule::class, ServiceSingletonModule::class,
      PurgeSingletonModule::class, PurgeSingletonProvider::class, SettingsSingletonModule::class,
      LockInfoSingletonModule::class, LockInfoSingletonProvider::class, LockStateModule::class,
      LockListSingletonModule::class, LockListSingletonProvider::class, LockSingletonModule::class,
      LockSingletonProvider::class, PinSingletonProvider::class, ServiceSingletonProvider::class
    ]
)
interface PadLockComponent {

  fun inject(application: PadLock)

  fun inject(receiver: BootReceiver)

  fun inject(service: PadLockService)

  fun inject(service: PadLockJobService)

  fun inject(base: PinBaseFragment)

  fun inject(dialog: PurgeAllDialog)

  fun inject(dialog: PurgeSingleItemDialog)

  fun inject(dialog: ConfirmDeleteDatabaseDialog)

  fun inject(dialog: ConfirmDeleteAllDialog)

  @CheckResult
  fun plusLockListComponent(): LockListComponent.Builder

  @CheckResult
  fun plusLockListItemComponent(): LockListItemComponent.Builder

  @CheckResult
  fun plusLockInfoComponent(): LockInfoComponent.Builder

  @CheckResult
  fun plusLockInfoItemComponent(): LockInfoItemComponent.Builder

  @CheckResult
  fun plusLockInfoExplainComponent(): LockInfoExplainComponent.Builder

  @CheckResult
  fun plusLockScreenComponent(): LockScreenComponent.Builder

  @CheckResult
  fun plusSettingsComponent(): SettingsComponent.Builder

  @CheckResult
  fun plusPauseComponent(): PauseComponent.Builder

  @CheckResult
  fun plusPurgeComponent(): PurgeComponent.Builder

  @CheckResult
  fun plusPurgeItemComponent(): PurgeItemComponent.Builder

  @CheckResult
  fun plusPinComponent(): PinComponent.Builder

  @CheckResult
  fun plusMainComponent(): MainComponent.Builder

  @CheckResult
  fun plusMainFragmentComponent(): MainFragmentComponent.Builder

  @Component.Builder
  interface Builder {

    @BindsInstance fun theming(theming: Theming): Builder

    @BindsInstance fun moshi(moshi: Moshi): Builder

    @BindsInstance fun enforcer(enforcer: Enforcer): Builder

    @BindsInstance fun application(application: Application): Builder

    @BindsInstance fun imageLoader(imageLoader: ImageLoader): Builder

    fun build(): PadLockComponent

  }

  @Module
  object PadLockProvider {

    private val clearAllBus = RxBus.create<ClearAllEvent>()
    private val clearDatabaseBus = RxBus.create<ClearDatabaseEvent>()
    private val recreateBus = RxBus.create<Unit>()
    private val settingsStateBus = RxBus.create<SwitchLockTypeEvent>()
    private val clearPinBus = RxBus.create<ClearPinEvent>()

    @JvmStatic
    @Provides
    internal fun provideClearPinBus(): EventBus<ClearPinEvent> = clearPinBus

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
    fun provideListStateCache(): Cache =
      ListStateUtil

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
    fun provideNotificationIcon(): Int = drawable.ic_padlock_notification

    @JvmStatic
    @Provides
    @Named("notification_color")
    @ColorRes
    fun provideNotificationColor(): Int = color.blue500
  }

  @Module
  abstract class PadLockModule {

    @Binds
    internal abstract fun bindClearDatabasePresenter(impl: ClearDatabasePresenterImpl): ClearDatabasePresenter

    @Binds
    internal abstract fun bindClearAllPresenter(impl: ClearAllPresenterImpl): ClearAllPresenter

    @Binds
    internal abstract fun bindClearPinPresenter(impl: ClearPinPresenterImpl): ClearPinPresenter

  }
}

