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
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Named

@Module
class PadLockProvider(context: Context,
    private val mainActivityClass: Class<out Activity>,
    private val lockScreenActivityClass: Class<out Activity>,
    private val recheckServiceClass: Class<out IntentService>) {

  private val appContext: Context = context.applicationContext


  @Provides
  internal fun provideContext(): Context = appContext

  @Provides
  @Named(
      "main_activity")
  internal fun provideMainActivityClass(): Class<out Activity> =
      mainActivityClass

  @Provides
  @Named(
      "lockscreen")
  internal fun provideLockScreenActivityClas(): Class<out Activity> =
      lockScreenActivityClass

  @Provides
  @Named(
      "recheck")
  internal fun provideRecheckServiceClass(): Class<out IntentService> =
      recheckServiceClass

  @Provides
  @Named(
      "computation")
  internal fun provideComputationScheduler(): Scheduler =
      Schedulers.computation()

  @Provides
  @Named("io")
  internal fun provideIOScheduler(): Scheduler =
      Schedulers.io()

  @Provides
  @Named("main")
  internal fun provideMainScheduler(): Scheduler =
      AndroidSchedulers.mainThread()
}
