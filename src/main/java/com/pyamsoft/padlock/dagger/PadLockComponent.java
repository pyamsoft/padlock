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

package com.pyamsoft.padlock.dagger;

import com.pyamsoft.padlock.dagger.list.LockInfoComponent;
import com.pyamsoft.padlock.dagger.list.LockListComponent;
import com.pyamsoft.padlock.dagger.lock.LockScreenComponent;
import com.pyamsoft.padlock.dagger.lock.PinEntryComponent;
import com.pyamsoft.padlock.dagger.main.MainComponent;
import com.pyamsoft.padlock.dagger.service.LockServiceComponent;
import com.pyamsoft.padlock.dagger.settings.SettingsComponent;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = PadLockModule.class) public interface PadLockComponent {

  // Subcomponent Settings
  SettingsComponent plusSettings();

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
}
