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

package com.pyamsoft.padlock.presenter;

import com.pyamsoft.padlock.presenter.iconloader.AppIconLoaderComponent;
import com.pyamsoft.padlock.presenter.list.LockInfoComponent;
import com.pyamsoft.padlock.presenter.list.LockListComponent;
import com.pyamsoft.padlock.presenter.lock.LockScreenComponent;
import com.pyamsoft.padlock.presenter.lock.PinEntryComponent;
import com.pyamsoft.padlock.presenter.main.MainComponent;
import com.pyamsoft.padlock.presenter.purge.PurgeComponent;
import com.pyamsoft.padlock.presenter.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.presenter.receiver.ReceiverModule;
import com.pyamsoft.padlock.presenter.service.LockServiceComponent;
import com.pyamsoft.padlock.presenter.settings.SettingsPreferenceComponent;
import com.pyamsoft.padlock.presenter.wrapper.JobSchedulerCompatModule;
import com.pyamsoft.padlock.presenter.wrapper.PackageManagerWrapperModule;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = {
    PadLockModule.class, PackageManagerWrapperModule.class, JobSchedulerCompatModule.class,
    ReceiverModule.class
}) public interface PadLockComponent {

  // Subcomponent Settings
  SettingsPreferenceComponent plusSettings();

  // Subcomponent LockService
  LockServiceComponent plusLockService();

  // Subcomponent Main
  MainComponent plusMain();

  // Subcomponent LockScreen
  LockScreenComponent plusLockScreen();

  // Subcomponent PinEntry
  PinEntryComponent plusPinEntry();

  // Subcomponent LockList
  LockListComponent plusLockList();

  // Subcomponent LockInfo
  LockInfoComponent plusLockInfo();

  // Subcomponent AppIconLoader
  AppIconLoaderComponent plusAppIconLoaderComponent();

  PurgeComponent plusPurgeComponent();

  // KLUDGE: For use only in the PadLock application class
  ApplicationInstallReceiver provideApplicationInstallReceiver();

  // KLUDGE: For use only in the PadLock application class
  PadLockPreferences providePreferences();
}