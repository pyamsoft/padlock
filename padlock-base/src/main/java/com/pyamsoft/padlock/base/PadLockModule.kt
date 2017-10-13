/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.base

import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.db.PadLockDBImpl
import com.pyamsoft.padlock.base.db.PadLockDBInsert
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.db.PadLockDBUpdate
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.preference.PadLockPreferencesImpl
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiverImpl
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompatImpl
import com.pyamsoft.padlock.base.wrapper.PackageActivityManager
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager
import com.pyamsoft.padlock.base.wrapper.PackageDrawableManager
import com.pyamsoft.padlock.base.wrapper.PackageLabelManager
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapperImpl
import dagger.Binds
import dagger.Module

@Module
abstract class PadLockModule {

  @Binds
  internal abstract fun provideApplicationInstallReceiver(
      impl: ApplicationInstallReceiverImpl): ApplicationInstallReceiver

  @Binds
  internal abstract fun provideMasterPinPreference(
      impl: PadLockPreferencesImpl): MasterPinPreferences

  @Binds
  internal abstract fun provideClearPreferences(
      impl: PadLockPreferencesImpl): ClearPreferences

  @Binds
  internal abstract fun provideInstallListenerPreferences(
      impl: PadLockPreferencesImpl): InstallListenerPreferences

  @Binds
  internal abstract fun provideLockListPreferences(
      impl: PadLockPreferencesImpl): LockListPreferences

  @Binds
  internal abstract fun provideLockScreenPreferences(
      impl: PadLockPreferencesImpl): LockScreenPreferences

  @Binds
  internal abstract fun provideOnboardingPreferences(
      impl: PadLockPreferencesImpl): OnboardingPreferences

  @Binds
  internal abstract fun providePackageActivityManager(
      impl: PackageManagerWrapperImpl): PackageActivityManager

  @Binds
  internal abstract fun providePackageLabelManager(
      impl: PackageManagerWrapperImpl): PackageLabelManager

  @Binds
  internal abstract fun providePackageApplicationManager(
      impl: PackageManagerWrapperImpl): PackageApplicationManager

  @Binds
  internal abstract fun providePackageDrawableManager(
      impl: PackageManagerWrapperImpl): PackageDrawableManager

  @Binds
  internal abstract fun provideJobSchedulerCompat(impl: JobSchedulerCompatImpl): JobSchedulerCompat

  @Binds
  internal abstract fun providePadLockInsert(impl: PadLockDBImpl): PadLockDBInsert

  @Binds
  internal abstract fun providePadLockQuery(impl: PadLockDBImpl): PadLockDBQuery

  @Binds
  internal abstract fun providePadLockUpdate(impl: PadLockDBImpl): PadLockDBUpdate

  @Binds
  internal abstract fun providePadLockDelete(impl: PadLockDBImpl): PadLockDBDelete

}