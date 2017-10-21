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

package com.pyamsoft.padlock.onboard.firstlaunch

import com.pyamsoft.padlock.onboard.firstlaunch.OnboardAcceptTermsPresenter.View
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class OnboardAcceptTermsPresenter @Inject internal constructor(
    private val interactor: OnboardAcceptTermsInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler) : SchedulerPresenter<View>(compScheduler,
    ioScheduler,
    mainScheduler) {

  fun acceptUsageTerms() {
    dispose {
      interactor.agreeToTerms()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ view?.onUsageTermsAccepted() }, {
            Timber.e(it, "onError")
          })
    }
  }

  interface View : UsageTermsCallback

  interface UsageTermsCallback {

    fun onUsageTermsAccepted()
  }
}
