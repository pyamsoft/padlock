/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.lock.screen

import com.pyamsoft.padlock.api.LockScreenInteractor
import com.pyamsoft.padlock.lock.screen.LockScreenInputPresenter.View
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockScreenInputPresenter @Inject internal constructor(
  private val interactor: LockScreenInteractor,
  @Named("computation") computationScheduler: Scheduler,
  @Named("main") mainScheduler: Scheduler,
  @Named("io") ioScheduler: Scheduler
) : SchedulerPresenter<View>(
    computationScheduler,
    ioScheduler, mainScheduler
) {

  override fun onCreate() {
    super.onCreate()
    initializeLockScreenType()
  }

  private fun initializeLockScreenType() {
    dispose {
      interactor.getLockScreenType()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              TYPE_PATTERN -> view?.onTypePattern()
              TYPE_TEXT -> view?.onTypeText()
              else -> throw IllegalArgumentException("Invalid enum: $it")
            }
          }, {
            Timber.e(it, "Error initializing lock screen type")
          })
    }
  }

  interface View : TypeCallback

  interface TypeCallback {

    fun onTypePattern()
    fun onTypeText()
  }
}
