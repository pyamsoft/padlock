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

package com.pyamsoft.padlock

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.PadLockModule
import com.pyamsoft.padlock.base.db.PadLockDBModule
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.queue.QueueModule
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompatModule
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapperModule
import com.pyamsoft.padlock.list.LockInfoComponent
import com.pyamsoft.padlock.list.LockInfoItem
import com.pyamsoft.padlock.list.LockListFragment
import com.pyamsoft.padlock.list.LockListItem
import com.pyamsoft.padlock.list.LockListSingletonModule
import com.pyamsoft.padlock.list.info.LockInfoModule
import com.pyamsoft.padlock.list.info.LockInfoSingletonModule
import com.pyamsoft.padlock.list.modify.LockStateModule
import com.pyamsoft.padlock.lock.LockEntryModule
import com.pyamsoft.padlock.lock.LockEntrySingletonModule
import com.pyamsoft.padlock.lock.LockScreenComponent
import com.pyamsoft.padlock.lock.helper.LockModule
import com.pyamsoft.padlock.lock.screen.LockScreenModule
import com.pyamsoft.padlock.lock.screen.LockScreenSingletonModule
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.main.MainModule
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.pin.PinEntryPatternFragment
import com.pyamsoft.padlock.pin.PinEntryTextFragment
import com.pyamsoft.padlock.pin.PinModule
import com.pyamsoft.padlock.purge.PurgeAllDialog
import com.pyamsoft.padlock.purge.PurgeFragment
import com.pyamsoft.padlock.purge.PurgeModule
import com.pyamsoft.padlock.purge.PurgeSingleItemDialog
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.service.RecheckService
import com.pyamsoft.padlock.service.ServiceModule
import com.pyamsoft.padlock.settings.ConfirmationDialog
import com.pyamsoft.padlock.settings.SettingsFragment
import com.pyamsoft.padlock.settings.SettingsModule
import com.pyamsoft.padlock.uicommon.AppIconLoader
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(PadLockModule::class, PackageManagerWrapperModule::class,
    JobSchedulerCompatModule::class, PadLockDBModule::class, LockModule::class, PinModule::class,
    ServiceModule::class, PurgeModule::class, MainModule::class, SettingsModule::class,
    LockInfoSingletonModule::class, LockStateModule::class, LockListSingletonModule::class,
    QueueModule::class, LockScreenSingletonModule::class, LockEntrySingletonModule::class))
interface PadLockComponent {

  fun inject(fragment: LockListFragment)

  fun inject(appIconLoader: AppIconLoader)

  fun inject(mainActivity: MainActivity)

  fun inject(fragment: PinEntryTextFragment)

  fun inject(pinEntryDialog: PinEntryDialog)

  fun inject(pinEntryPatternFragment: PinEntryPatternFragment)

  fun inject(purgeFragment: PurgeFragment)

  fun inject(padLockService: PadLockService)

  fun inject(settingsFragment: SettingsFragment)

  fun inject(recheckService: RecheckService)

  fun inject(purgeAllDialog: PurgeAllDialog)

  fun inject(purgeSingleItemDialog: PurgeSingleItemDialog)

  fun inject(confirmationDialog: ConfirmationDialog)

  fun inject(viewHolder: LockInfoItem.ViewHolder)

  fun inject(viewHolder: LockListItem.ViewHolder)

  // To be used directly by PadLockSingleInitProvider
  @CheckResult
  fun provideInstallListenerPreferences(): InstallListenerPreferences

  // To be used directly by PadLockSingleInitProvider
  @CheckResult
  fun provideApplicationInstallReceiver(): ApplicationInstallReceiver

  @CheckResult
  fun plusLockInfoComponent(module: LockInfoModule): LockInfoComponent

  @CheckResult
  fun plusLockScreenComponent(screenModule: LockScreenModule,
      entryModule: LockEntryModule): LockScreenComponent
}
