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
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.moshi.MoshiPersister
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.padlock.PadLockComponent.PadLockModule
import com.pyamsoft.padlock.PadLockComponent.PadLockProvider
import com.pyamsoft.padlock.R.color
import com.pyamsoft.padlock.R.drawable
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ForegroundEvent
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
import com.pyamsoft.padlock.pin.ClearPinPresenter
import com.pyamsoft.padlock.pin.ClearPinPresenterImpl
import com.pyamsoft.padlock.pin.ClearPinPresenterImpl.ClearPinEvent
import com.pyamsoft.padlock.pin.ConfirmPinPresenter
import com.pyamsoft.padlock.pin.ConfirmPinPresenterImpl
import com.pyamsoft.padlock.pin.ConfirmPinPresenterImpl.CheckPinEvent
import com.pyamsoft.padlock.pin.CreatePinPresenter
import com.pyamsoft.padlock.pin.CreatePinPresenterImpl
import com.pyamsoft.padlock.pin.CreatePinPresenterImpl.CreatePinEvent
import com.pyamsoft.padlock.pin.PinComponent
import com.pyamsoft.padlock.pin.PinSingletonModule
import com.pyamsoft.padlock.pin.PinSingletonProvider
import com.pyamsoft.padlock.purge.PurgeAllDialog
import com.pyamsoft.padlock.purge.PurgeAllPresenter
import com.pyamsoft.padlock.purge.PurgeAllPresenterImpl
import com.pyamsoft.padlock.purge.PurgeAllPresenterImpl.PurgeAllEvent
import com.pyamsoft.padlock.purge.PurgeComponent
import com.pyamsoft.padlock.purge.PurgeItemComponent
import com.pyamsoft.padlock.purge.PurgeSingleItemDialog
import com.pyamsoft.padlock.purge.PurgeSinglePresenter
import com.pyamsoft.padlock.purge.PurgeSinglePresenterImpl
import com.pyamsoft.padlock.purge.PurgeSinglePresenterImpl.PurgeSingleEvent
import com.pyamsoft.padlock.purge.PurgeSingletonModule
import com.pyamsoft.padlock.receiver.BootReceiver
import com.pyamsoft.padlock.service.ForegroundEventPresenter
import com.pyamsoft.padlock.service.ForegroundEventPresenterImpl
import com.pyamsoft.padlock.service.LockServicePresenter
import com.pyamsoft.padlock.service.LockServicePresenterImpl
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.service.PermissionPresenter
import com.pyamsoft.padlock.service.PermissionPresenterImpl
import com.pyamsoft.padlock.service.RecheckPresenter
import com.pyamsoft.padlock.service.RecheckPresenterImpl
import com.pyamsoft.padlock.service.RecheckPresenterImpl.RecheckEvent
import com.pyamsoft.padlock.service.ScreenStatePresenter
import com.pyamsoft.padlock.service.ScreenStatePresenterImpl
import com.pyamsoft.padlock.service.ServiceActionPresenter
import com.pyamsoft.padlock.service.ServiceActionPresenterImpl
import com.pyamsoft.padlock.service.ServiceActionPresenterImpl.ServicePauseEvent
import com.pyamsoft.padlock.service.ServiceFinishPresenter
import com.pyamsoft.padlock.service.ServiceFinishPresenterImpl
import com.pyamsoft.padlock.service.ServiceFinishPresenterImpl.ServiceFinishEvent
import com.pyamsoft.padlock.service.ServicePausePresenter
import com.pyamsoft.padlock.service.ServicePausePresenterImpl
import com.pyamsoft.padlock.service.ServiceSingletonModule
import com.pyamsoft.padlock.service.ServiceStartPresenter
import com.pyamsoft.padlock.service.ServiceStartPresenterImpl
import com.pyamsoft.padlock.service.job.PadLockJobService
import com.pyamsoft.padlock.settings.ClearAllPresenter
import com.pyamsoft.padlock.settings.ClearAllPresenterImpl
import com.pyamsoft.padlock.settings.ClearAllPresenterImpl.ClearAllEvent
import com.pyamsoft.padlock.settings.ClearDatabasePresenter
import com.pyamsoft.padlock.settings.ClearDatabasePresenterImpl
import com.pyamsoft.padlock.settings.ClearDatabasePresenterImpl.ClearDatabaseEvent
import com.pyamsoft.padlock.settings.ConfirmDeleteAllDialog
import com.pyamsoft.padlock.settings.ConfirmDeleteDatabaseDialog
import com.pyamsoft.padlock.settings.SettingsComponent
import com.pyamsoft.padlock.settings.SettingsSingletonModule
import com.pyamsoft.padlock.settings.SwitchLockTypePresenter
import com.pyamsoft.padlock.settings.SwitchLockTypePresenterImpl
import com.pyamsoft.padlock.settings.SwitchLockTypePresenterImpl.SwitchLockTypeEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import java.io.File
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
      PadLockProvider::class, PadLockModule::class, BaseModule::class, BaseProvider::class,
      DatabaseProvider::class, PinSingletonModule::class, ServiceSingletonModule::class,
      PurgeSingletonModule::class, SettingsSingletonModule::class,
      LockInfoSingletonModule::class, LockInfoSingletonProvider::class, LockStateModule::class,
      LockListSingletonModule::class, LockListSingletonProvider::class, LockSingletonModule::class,
      LockSingletonProvider::class, PinSingletonProvider::class
    ]
)
interface PadLockComponent {

  fun inject(application: PadLock)

  fun inject(receiver: BootReceiver)

  fun inject(service: PadLockService)

  fun inject(service: PadLockJobService)

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

    @BindsInstance
    @CheckResult
    fun theming(theming: Theming): Builder

    @BindsInstance
    @CheckResult
    fun moshi(moshi: Moshi): Builder

    @BindsInstance
    @CheckResult
    fun enforcer(enforcer: Enforcer): Builder

    @BindsInstance
    @CheckResult
    fun application(application: Application): Builder

    @BindsInstance
    @CheckResult
    fun imageLoader(imageLoader: ImageLoader): Builder

    @CheckResult
    fun build(): PadLockComponent

  }

  @Module
  object PadLockProvider {

    private val clearAllBus = RxBus.create<ClearAllEvent>()
    private val clearDatabaseBus = RxBus.create<ClearDatabaseEvent>()
    private val settingsStateBus = RxBus.create<SwitchLockTypeEvent>()

    private val recheckBus = RxBus.create<RecheckEvent>()

    private val servicePauseBus = RxBus.create<ServicePauseEvent>()
    private val serviceFinishBus = RxBus.create<ServiceFinishEvent>()
    private val foregroundEventBus = RxBus.create<ForegroundEvent>()

    private val clearPinBus = RxBus.create<ClearPinEvent>()
    private val checkPinBus = RxBus.create<CheckPinEvent>()
    private val createPinBus = RxBus.create<CreatePinEvent>()

    private val purgeSingleBus = RxBus.create<PurgeSingleEvent>()
    private val purgeAllBus = RxBus.create<PurgeAllEvent>()

    @JvmStatic
    @Provides
    internal fun providePurgeSingleBus(): EventBus<PurgeSingleEvent> = purgeSingleBus

    @JvmStatic
    @Provides
    internal fun providePurgeAllBus(): EventBus<PurgeAllEvent> = purgeAllBus

    @JvmStatic
    @Provides
    internal fun provideForegroundEventBus(): EventBus<ForegroundEvent> = foregroundEventBus

    @JvmStatic
    @Provides
    internal fun provideServiceFinishBus(): EventBus<ServiceFinishEvent> = serviceFinishBus

    @JvmStatic
    @Provides
    internal fun provideServicePauseBus(): EventBus<ServicePauseEvent> = servicePauseBus

    @JvmStatic
    @Provides
    internal fun provideRecheckBus(): EventBus<RecheckEvent> = recheckBus

    @JvmStatic
    @Provides
    internal fun provideCreatePinBus(): EventBus<CreatePinEvent> = createPinBus

    @JvmStatic
    @Provides
    internal fun provideClearPinBus(): EventBus<ClearPinEvent> = clearPinBus

    @JvmStatic
    @Provides
    internal fun provideCheckPinBus(): EventBus<CheckPinEvent> = checkPinBus

    @JvmStatic
    @Provides
    internal fun provideClearAllBus(): EventBus<ClearAllEvent> = clearAllBus

    @JvmStatic
    @Provides
    internal fun provideClearDatabaseBus(): EventBus<ClearDatabaseEvent> = clearDatabaseBus

    @JvmStatic
    @Provides
    internal fun provideLockTypeBus(): EventBus<SwitchLockTypeEvent> = settingsStateBus

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
    fun provideNotificationIcon(): Int = drawable.ic_padlock_notification

    @JvmStatic
    @Provides
    @Named("notification_color")
    @ColorRes
    fun provideNotificationColor(): Int = color.blue500

    @JvmStatic
    @Provides
    @Named("repo_purge_list")
    internal fun providePurgeRepo(
      context: Context,
      moshi: Moshi
    ): Repo<List<String>> {
      val type = Types.newParameterizedType(List::class.java, String::class.java)
      return newRepoBuilder<List<String>>()
          .memoryCache(30, MINUTES)
          .persister(
              2, HOURS,
              File(context.cacheDir, "repo-purge-list"),
              MoshiPersister.create(moshi, type)
          )
          .build()
    }
  }

  @Module
  abstract class PadLockModule {

    @Binds
    internal abstract fun bindClearDatabasePresenter(impl: ClearDatabasePresenterImpl): ClearDatabasePresenter

    @Binds
    internal abstract fun bindClearAllPresenter(impl: ClearAllPresenterImpl): ClearAllPresenter

    @Binds
    internal abstract fun bindSwitchLockTypePresenter(impl: SwitchLockTypePresenterImpl): SwitchLockTypePresenter

    @Binds
    internal abstract fun bindCreatePinPresenter(impl: CreatePinPresenterImpl): CreatePinPresenter

    @Binds
    internal abstract fun bindClearPinPresenter(impl: ClearPinPresenterImpl): ClearPinPresenter

    @Binds
    internal abstract fun bindCheckPinPresenter(impl: ConfirmPinPresenterImpl): ConfirmPinPresenter

    @Binds
    internal abstract fun bindRecheckPresenter(impl: RecheckPresenterImpl): RecheckPresenter

    @Binds
    internal abstract fun bindServiceActionPresenter(impl: ServiceActionPresenterImpl): ServiceActionPresenter

    @Binds
    internal abstract fun bindServiceFinishPresenter(impl: ServiceFinishPresenterImpl): ServiceFinishPresenter

    @Binds
    internal abstract fun bindPermissionPresenter(impl: PermissionPresenterImpl): PermissionPresenter

    @Binds
    internal abstract fun bindForegroundPresenter(impl: ForegroundEventPresenterImpl): ForegroundEventPresenter

    @Binds
    internal abstract fun bindServicePresenter(impl: LockServicePresenterImpl): LockServicePresenter

    @Binds
    internal abstract fun bindScreenPresenter(impl: ScreenStatePresenterImpl): ScreenStatePresenter

    @Binds
    internal abstract fun bindPausePresenter(impl: ServicePausePresenterImpl): ServicePausePresenter

    @Binds
    internal abstract fun bindStartPresenter(impl: ServiceStartPresenterImpl): ServiceStartPresenter

    @Binds
    internal abstract fun bindPurgeSinglePresenter(impl: PurgeSinglePresenterImpl): PurgeSinglePresenter

    @Binds
    internal abstract fun bindPurgeAllPresenter(impl: PurgeAllPresenterImpl): PurgeAllPresenter

  }
}

