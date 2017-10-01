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

package com.pyamsoft.padlock.lock.screen

import android.support.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class LockScreenModule(private val packageName: String, private val activityName: String) {

  @Provides
  @CheckResult internal fun providePresenter(
      inputPresenter: LockScreenInputPresenter,
      bus: EventBus<CloseOldEvent>,
      interactor: LockScreenInteractor, @Named("computation") compScheduler: Scheduler,
      @Named("main") mainScheduler: Scheduler,
      @Named("io") ioScheduler: Scheduler): LockScreenPresenter {
    return LockScreenPresenter(inputPresenter, packageName, activityName, bus, interactor,
        compScheduler, ioScheduler, mainScheduler)
  }
}

