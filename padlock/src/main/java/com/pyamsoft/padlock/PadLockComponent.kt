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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.base.BaseModule
import com.pyamsoft.padlock.base.BaseProvider
import com.pyamsoft.padlock.base.database.DatabaseProvider
import com.pyamsoft.padlock.list.ListListComponent
import com.pyamsoft.padlock.list.LockInfoComponent
import com.pyamsoft.padlock.list.LockInfoItem
import com.pyamsoft.padlock.list.LockInfoProvider
import com.pyamsoft.padlock.list.LockListItem
import com.pyamsoft.padlock.list.LockListProvider
import com.pyamsoft.padlock.list.LockListSingletonModule
import com.pyamsoft.padlock.list.LockListSingletonProvider
import com.pyamsoft.padlock.list.info.LockInfoSingletonModule
import com.pyamsoft.padlock.list.info.LockInfoSingletonProvider
import com.pyamsoft.padlock.list.modify.LockStateModule
import com.pyamsoft.padlock.lock.LockEntryModule
import com.pyamsoft.padlock.lock.LockScreenComponent
import com.pyamsoft.padlock.lock.LockSingletonModule
import com.pyamsoft.padlock.lock.LockSingletonProvider
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.main.MainViewComponent
import com.pyamsoft.padlock.main.MainViewProvider
import com.pyamsoft.padlock.pin.PinBaseFragment
import com.pyamsoft.padlock.pin.PinComponent
import com.pyamsoft.padlock.pin.PinProvider
import com.pyamsoft.padlock.pin.PinSingletonModule
import com.pyamsoft.padlock.pin.PinSingletonProvider
import com.pyamsoft.padlock.purge.PurgeAllDialog
import com.pyamsoft.padlock.purge.PurgeComponent
import com.pyamsoft.padlock.purge.PurgeItemComponent
import com.pyamsoft.padlock.purge.PurgeItemProvider
import com.pyamsoft.padlock.purge.PurgeProvider
import com.pyamsoft.padlock.purge.PurgeSingleItemDialog
import com.pyamsoft.padlock.purge.PurgeSingletonModule
import com.pyamsoft.padlock.purge.PurgeSingletonProvider
import com.pyamsoft.padlock.receiver.BootReceiver
import com.pyamsoft.padlock.service.PadLockJobService
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.service.PauseComponent
import com.pyamsoft.padlock.service.PauseProvider
import com.pyamsoft.padlock.service.ServiceSingletonModule
import com.pyamsoft.padlock.service.ServiceSingletonProvider
import com.pyamsoft.padlock.settings.ConfirmationDialog
import com.pyamsoft.padlock.settings.SettingsComponent
import com.pyamsoft.padlock.settings.SettingsProvider
import com.pyamsoft.padlock.settings.SettingsSingletonModule
import com.pyamsoft.padlock.settings.SettingsSingletonProvider
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
      PadLockProvider::class, BaseModule::class, DatabaseProvider::class,
      PinSingletonModule::class, ServiceSingletonModule::class, PurgeSingletonModule::class,
      PurgeSingletonProvider::class, SettingsSingletonModule::class, LockInfoSingletonModule::class,
      LockInfoSingletonProvider::class, LockStateModule::class, LockListSingletonModule::class,
      LockListSingletonProvider::class, LockSingletonModule::class, LockSingletonProvider::class,
      SettingsSingletonProvider::class, BaseProvider::class, PinSingletonProvider::class,
      ServiceSingletonProvider::class
    ]
)
interface PadLockComponent {

  fun inject(application: PadLock)

  fun inject(activity: MainActivity)

  fun inject(receiver: BootReceiver)

  fun inject(service: PadLockService)

  fun inject(service: PadLockJobService)

  fun inject(base: PinBaseFragment)

  fun inject(dialog: PurgeAllDialog)

  fun inject(dialog: PurgeSingleItemDialog)

  fun inject(dialog: ConfirmationDialog)

  fun inject(viewHolder: LockInfoItem.ViewHolder)

  fun inject(viewHolder: LockListItem.ViewHolder)

  @CheckResult
  fun plusLockListComponent(provider: LockListProvider): ListListComponent

  @CheckResult
  fun plusLockInfoComponent(provider: LockInfoProvider): LockInfoComponent

  @CheckResult
  fun plusLockScreenComponent(entryModule: LockEntryModule): LockScreenComponent

  @CheckResult
  fun plusSettingsComponent(provider: SettingsProvider): SettingsComponent

  @CheckResult
  fun plusPauseComponent(provider: PauseProvider): PauseComponent

  @CheckResult
  fun plusPurgeComponent(provider: PurgeProvider): PurgeComponent

  @CheckResult
  fun plusPurgeItemComponent(provider: PurgeItemProvider): PurgeItemComponent

  @CheckResult
  fun plusPinComponent(provider: PinProvider): PinComponent

  @CheckResult
  fun plusMainViewComponent(provider: MainViewProvider): MainViewComponent
}
