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

package com.pyamsoft.padlock.base

import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.padlock.api.packagemanager.PackageApplicationManager
import com.pyamsoft.padlock.api.packagemanager.PackageLabelManager
import com.pyamsoft.padlock.api.preferences.ClearPreferences
import com.pyamsoft.padlock.api.preferences.InstallListenerPreferences
import com.pyamsoft.padlock.api.preferences.LockListPreferences
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.api.preferences.MasterPinPreferences
import com.pyamsoft.padlock.api.preferences.ServicePreferences
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.base.jobs.JobSchedulerCompatImpl
import dagger.Binds
import dagger.Module

@Module
abstract class BaseModule {

  @Binds
  internal abstract fun provideMasterPinPreference(
    impl: PadLockPreferencesImpl
  ): MasterPinPreferences

  @Binds
  internal abstract fun provideClearPreferences(
    impl: PadLockPreferencesImpl
  ): ClearPreferences

  @Binds
  internal abstract fun provideInstallListenerPreferences(
    impl: PadLockPreferencesImpl
  ): InstallListenerPreferences

  @Binds
  internal abstract fun provideLockListPreferences(
    impl: PadLockPreferencesImpl
  ): LockListPreferences

  @Binds
  internal abstract fun provideLockScreenPreferences(
    impl: PadLockPreferencesImpl
  ): LockScreenPreferences

  @Binds
  internal abstract fun provideServicePreferences(
    impl: PadLockPreferencesImpl
  ): ServicePreferences

  @Binds
  internal abstract fun providePackageActivityManager(
    impl: PackageManagerWrapperImpl
  ): PackageActivityManager

  @Binds
  internal abstract fun providePackageLabelManager(
    impl: PackageManagerWrapperImpl
  ): PackageLabelManager

  @Binds
  internal abstract fun providePackageApplicationManager(
    impl: PackageManagerWrapperImpl
  ): PackageApplicationManager

  @Binds
  internal abstract fun provideJobSchedulerCompat(
    impl: JobSchedulerCompatImpl
  ): JobSchedulerCompat
}
