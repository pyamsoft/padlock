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

import android.app.Activity
import android.app.IntentService
import android.content.Context
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiverImpl
import com.pyamsoft.padlock.base.wrapper.PackageLabelManager
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Named
import javax.inject.Singleton

@Module
class PadLockModule(context: Context,
    private val mainActivityClass: Class<out Activity>,
    private val lockScreenActivityClass: Class<out Activity>,
    private val recheckServiceClass: Class<out IntentService>) {

  private val appContext: Context = context.applicationContext
  private val preferences: PadLockPreferencesImpl = PadLockPreferencesImpl(appContext)

  @Singleton
  @Provides
  @CheckResult internal fun provideContext(): Context = appContext

  @Singleton
  @Provides
  @CheckResult internal fun provideMasterPinPreference(): MasterPinPreferences =
      preferences

  @Singleton
  @Provides
  @CheckResult internal fun provideClearPreferences(): ClearPreferences =
      preferences

  @Singleton
  @Provides
  @CheckResult internal fun provideInstallListenerPreferences(): InstallListenerPreferences =
      preferences

  @Singleton
  @Provides
  @CheckResult internal fun provideLockListPreferences(): LockListPreferences =
      preferences

  @Singleton
  @Provides
  @CheckResult internal fun provideLockScreenPreferences(): LockScreenPreferences =
      preferences

  @Singleton
  @Provides
  @CheckResult internal fun provideOnboardingPreferences(): OnboardingPreferences =
      preferences

  @Singleton
  @Provides
  @CheckResult internal fun provideApplicationInstallReceiver(
      packageManagerWrapper: PackageLabelManager,
      @Named("io") ioScheduler: Scheduler,
      @Named("main") mainThreadScheduler: Scheduler,
      @Named("main_activity") mainActivityClass: Class<out Activity>,
      @Named("cache_purge") purgeCache: Cache): ApplicationInstallReceiver =
      ApplicationInstallReceiverImpl(appContext, packageManagerWrapper, ioScheduler,
          mainThreadScheduler, mainActivityClass, purgeCache)

  @Singleton
  @Provides
  @Named(
      "main_activity")
  @CheckResult internal fun provideMainActivityClass(): Class<out Activity> =
      mainActivityClass

  @Singleton
  @Provides
  @Named(
      "lockscreen")
  @CheckResult internal fun provideLockScreenActivityClas(): Class<out Activity> =
      lockScreenActivityClass

  @Singleton
  @Provides
  @Named(
      "recheck")
  @CheckResult internal fun provideRecheckServiceClass(): Class<out IntentService> =
      recheckServiceClass

  @Singleton
  @Provides
  @Named(
      "computation")
  @CheckResult internal fun provideComputationScheduler(): Scheduler =
      Schedulers.computation()

  @Singleton
  @Provides
  @Named("io")
  @CheckResult internal fun provideIOScheduler(): Scheduler =
      Schedulers.io()

  @Singleton
  @Provides
  @Named("main")
  @CheckResult internal fun provideMainScheduler(): Scheduler =
      AndroidSchedulers.mainThread()
}
