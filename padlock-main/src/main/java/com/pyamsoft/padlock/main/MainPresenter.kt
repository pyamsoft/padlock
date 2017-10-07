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

package com.pyamsoft.padlock.main

import com.pyamsoft.padlock.main.MainPresenter.MainCallback
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class MainPresenter @Inject internal constructor(private val interactor: MainInteractor,
    @Named("computation") computationScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler) : SchedulerPresenter<MainCallback>(
    computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onBind(v: MainCallback) {
    super.onBind(v)
    showOnboardingOrDefault(v::onShowDefaultPage, v::onShowOnboarding)
  }

  private fun showOnboardingOrDefault(onShowDefaultPage: () -> Unit, onShowOnboarding: () -> Unit) {
    dispose {
      interactor.isOnboardingComplete()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onShowDefaultPage()
            } else {
              onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  interface MainCallback {

    fun onShowDefaultPage()

    fun onShowOnboarding()

  }
}
