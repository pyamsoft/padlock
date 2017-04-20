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

package com.pyamsoft.padlock;

import android.support.annotation.CheckResult;
import com.pyamsoft.padlock.base.PadLockModule;
import com.pyamsoft.padlock.base.db.PadLockDBModule;
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompatModule;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapperModule;
import com.pyamsoft.padlock.list.LockInfoComponent;
import com.pyamsoft.padlock.list.LockListComponent;
import com.pyamsoft.padlock.loader.LoaderComponent;
import com.pyamsoft.padlock.lock.LockScreenComponent;
import com.pyamsoft.padlock.main.MainComponent;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardFirstLaunchComponent;
import com.pyamsoft.padlock.onboard.list.OnboardListComponent;
import com.pyamsoft.padlock.pin.PinEntryComponent;
import com.pyamsoft.padlock.purge.PurgeComponent;
import com.pyamsoft.padlock.service.LockServiceComponent;
import com.pyamsoft.padlock.settings.SettingsPreferenceComponent;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = {
    PadLockModule.class, PackageManagerWrapperModule.class, JobSchedulerCompatModule.class,
    PadLockDBModule.class
}) public interface PadLockComponent {

  @CheckResult LockInfoComponent plusLockInfoComponent();

  @CheckResult LockListComponent plusLockListComponent();

  @CheckResult LockScreenComponent plusLockScreenComponent();

  @CheckResult PinEntryComponent plusPinEntryComponent();

  @CheckResult MainComponent plusMainComponent();

  @CheckResult OnboardFirstLaunchComponent plusOnboardFirstLaunchComponent();

  @CheckResult OnboardListComponent plusOnboardListComponent();

  @CheckResult PurgeComponent plusPurgeComponent();

  @CheckResult LockServiceComponent plusLockServiceComponent();

  @CheckResult SettingsPreferenceComponent plusSettingsPreferenceComponent();

  @CheckResult LoaderComponent plusLoaderComponent();

  // To be used directly by PadLockSingleInitProvider
  @CheckResult InstallListenerPreferences provideInstallListenerPreferences();

  // To be used directly by PadLockSingleInitProvider
  @CheckResult ApplicationInstallReceiver provideApplicationInstallReceiver();
}
