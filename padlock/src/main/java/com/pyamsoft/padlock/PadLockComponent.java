/*
 * Copyright 2016 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock;

import com.pyamsoft.padlock.base.PadLockModule;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDBModule;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.base.receiver.ReceiverModule;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompatModule;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapperModule;
import com.pyamsoft.padlock.iconloader.AppIconLoaderInteractorModule;
import com.pyamsoft.padlock.list.LockInfoComponent;
import com.pyamsoft.padlock.list.LockInfoSingletonModule;
import com.pyamsoft.padlock.list.LockListComponent;
import com.pyamsoft.padlock.list.LockListSingletonModule;
import com.pyamsoft.padlock.lock.LockScreenComponent;
import com.pyamsoft.padlock.lock.LockScreenSingletonModule;
import com.pyamsoft.padlock.lock.common.LockTypeSingletonModule;
import com.pyamsoft.padlock.lock.master.MasterPinModule;
import com.pyamsoft.padlock.main.MainComponent;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardFirstLaunchComponent;
import com.pyamsoft.padlock.onboard.list.OnboardListComponent;
import com.pyamsoft.padlock.pin.PinEntryComponent;
import com.pyamsoft.padlock.pin.PinEntrySingletonModule;
import com.pyamsoft.padlock.purge.PurgeComponent;
import com.pyamsoft.padlock.purge.PurgeSingletonModule;
import com.pyamsoft.padlock.service.LockServiceComponent;
import com.pyamsoft.padlock.service.LockServiceStateModule;
import com.pyamsoft.padlock.settings.SettingsPreferenceComponent;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = {
    PadLockModule.class, PackageManagerWrapperModule.class, JobSchedulerCompatModule.class,
    PadLockDBModule.class, ReceiverModule.class, LockListSingletonModule.class,
    LockInfoSingletonModule.class, PurgeSingletonModule.class, AppIconLoaderInteractorModule.class,
    LockScreenSingletonModule.class, PinEntrySingletonModule.class, MasterPinModule.class,
    LockServiceStateModule.class, LockTypeSingletonModule.class
}) public interface PadLockComponent {

  LockInfoComponent plusLockInfoComponent();

  LockListComponent plusLockListComponent();

  LockScreenComponent plusLockScreenComponent();

  PinEntryComponent plusPinEntryComponent();

  MainComponent plusMainComponent();

  OnboardFirstLaunchComponent plusOnboardFirstLaunchComponent();

  OnboardListComponent plusOnboardListComponent();

  PurgeComponent plusPurgeComponent();

  LockServiceComponent plusLockServiceComponent();

  SettingsPreferenceComponent plusSettingsPreferenceComponent();

  // To be used directly by PadLockSingleInitProvider
  PadLockPreferences providePreferences();

  // To be used directly by PadLockSingleInitProvider
  ApplicationInstallReceiver provideApplicationInstallReceiver();
}
