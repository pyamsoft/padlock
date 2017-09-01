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

package com.pyamsoft.padlock.main

import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class MainPresenter @Inject internal constructor(private val interactor: MainInteractor,
    @Named("computation") computationScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler) : SchedulerPresenter<Unit>(
    computationScheduler,
    ioScheduler, mainScheduler) {

  fun showOnboardingOrDefault(onShowDefaultPage: () -> Unit, onShowOnboarding: () -> Unit) {
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
}
