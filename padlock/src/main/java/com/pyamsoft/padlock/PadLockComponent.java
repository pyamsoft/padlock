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
import com.pyamsoft.padlock.list.LockInfoDialog;
import com.pyamsoft.padlock.list.LockInfoItem;
import com.pyamsoft.padlock.list.LockListFragment;
import com.pyamsoft.padlock.list.LockListItem;
import com.pyamsoft.padlock.loader.AppIconLoader;
import com.pyamsoft.padlock.lock.LockScreenActivity;
import com.pyamsoft.padlock.lock.LockScreenBaseFragment;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardAcceptTermsFragment;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardEnableServiceFragment;
import com.pyamsoft.padlock.onboard.list.OnboardListDialog;
import com.pyamsoft.padlock.pin.PinEntryDialog;
import com.pyamsoft.padlock.pin.PinEntryPatternFragment;
import com.pyamsoft.padlock.pin.PinEntryTextFragment;
import com.pyamsoft.padlock.purge.PurgeFragment;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.padlock.settings.SettingsFragment;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = {
    PadLockModule.class, PackageManagerWrapperModule.class, JobSchedulerCompatModule.class,
    PadLockDBModule.class
}) public interface PadLockComponent {

  void inject(LockInfoDialog dialog);

  void inject(LockInfoItem lockInfoItem);

  void inject(LockListFragment fragment);

  void inject(LockListItem lockListItem);

  void inject(AppIconLoader appIconLoader);

  void inject(LockScreenActivity lockScreenActivity);

  void inject(LockScreenBaseFragment lockScreenBaseFragment);

  void inject(MainActivity mainActivity);

  void inject(OnboardAcceptTermsFragment onboardAcceptTermsFragment);

  void inject(OnboardEnableServiceFragment onboardEnableServiceFragment);

  void inject(OnboardListDialog onboardListDialog);

  void inject(PinEntryTextFragment fragment);

  void inject(PinEntryDialog pinEntryDialog);

  void inject(PinEntryPatternFragment pinEntryPatternFragment);

  void inject(PurgeFragment purgeFragment);

  void inject(PadLockService padLockService);

  void inject(SettingsFragment settingsFragment);

  // To be used directly by PadLockSingleInitProvider
  @CheckResult InstallListenerPreferences provideInstallListenerPreferences();

  // To be used directly by PadLockSingleInitProvider
  @CheckResult ApplicationInstallReceiver provideApplicationInstallReceiver();
}
