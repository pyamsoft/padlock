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
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Named
import javax.inject.Singleton

@Module class PadLockModule(context: Context,
    private val mainActivityClass: Class<out Activity>,
    private val lockScreenActivityClass: Class<out Activity>,
    private val recheckServiceClass: Class<out IntentService>) {

  private val appContext: Context = context.applicationContext
  private val preferences: PadLockPreferencesImpl = PadLockPreferencesImpl(appContext)

  @Singleton @Provides @CheckResult internal fun provideContext(): Context {
    return appContext
  }

  @Singleton @Provides @CheckResult internal fun provideMasterPinPreference(): MasterPinPreferences {
    return preferences
  }

  @Singleton @Provides @CheckResult internal fun provideClearPreferences(): ClearPreferences {
    return preferences
  }

  @Singleton @Provides @CheckResult internal fun provideInstallListenerPreferences(): InstallListenerPreferences {
    return preferences
  }

  @Singleton @Provides @CheckResult internal fun provideLockListPreferences(): LockListPreferences {
    return preferences
  }

  @Singleton @Provides @CheckResult internal fun provideLockScreenPreferences(): LockScreenPreferences {
    return preferences
  }

  @Singleton @Provides @CheckResult internal fun provideOnboardingPreferences(): OnboardingPreferences {
    return preferences
  }

  @Singleton @Provides @CheckResult internal fun provideApplicationInstallReceiver(
      packageManagerWrapper: PackageLabelManager,
      @Named("computation") computationScheduler: Scheduler,
      @Named("io") ioScheduler: Scheduler,
      @Named("main") mainThreadScheduler: Scheduler,
      @Named("main_activity") mainActivityClass: Class<out Activity>): ApplicationInstallReceiver {
    return ApplicationInstallReceiverImpl(appContext, packageManagerWrapper, computationScheduler,
        ioScheduler, mainThreadScheduler, mainActivityClass)
  }

  @Singleton @Provides @Named(
      "main_activity") @CheckResult internal fun provideMainActivityClass(): Class<out Activity> {
    return mainActivityClass
  }

  @Singleton @Provides @Named(
      "lockscreen") @CheckResult internal fun provideLockScreenActivityClas(): Class<out Activity> {
    return lockScreenActivityClass
  }

  @Singleton @Provides @Named(
      "recheck") @CheckResult internal fun provideRecheckServiceClass(): Class<out IntentService> {
    return recheckServiceClass
  }

  @Singleton @Provides @Named(
      "computation") @CheckResult internal fun provideComputationScheduler(): Scheduler {
    return Schedulers.computation()
  }

  @Singleton @Provides @Named("io") @CheckResult internal fun provideIOScheduler(): Scheduler {
    return Schedulers.io()
  }

  @Singleton @Provides @Named("main") @CheckResult internal fun provideMainScheduler(): Scheduler {
    return AndroidSchedulers.mainThread()
  }
}
