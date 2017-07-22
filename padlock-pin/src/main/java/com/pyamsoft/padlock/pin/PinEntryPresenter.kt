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

package com.pyamsoft.padlock.pin

import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PinEntryPresenter @Inject internal constructor(private val interactor: PinEntryInteractor,
    @Named("obs") obsScheduler: Scheduler,
    @Named("sub") subScheduler: Scheduler) : SchedulerPresenter(obsScheduler, subScheduler) {

  fun submit(currentAttempt: String, reEntryAttempt: String, hint: String,
      onSubmitSuccess: (Boolean) -> Unit, onSubmitFailure: (Boolean) -> Unit,
      onSubmitError: (Throwable) -> Unit) {
    disposeOnStop(interactor.submitPin(currentAttempt, reEntryAttempt, hint)
        .subscribeOn(backgroundScheduler)
        .observeOn(foregroundScheduler)
        .subscribe({
          val creating = (it.type() === PinEntryEvent.Type.TYPE_CREATE)
          if (it.complete()) {
            onSubmitSuccess(creating)
          } else {
            onSubmitFailure(creating)
          }
        }, {
          Timber.e(it, "attemptPinSubmission onError")
          onSubmitError(it)
        }))
  }

  fun checkMasterPinPresent(onMasterPinMissing: () -> Unit, onMasterPinPresent: () -> Unit) {
    disposeOnStop(interactor.hasMasterPin()
        .subscribeOn(backgroundScheduler)
        .observeOn(foregroundScheduler)
        .subscribe({
          if (it) {
            onMasterPinPresent()
          } else {
            onMasterPinMissing()
          }
        }, { Timber.e(it, "onError checkMasterPinPresent") }))
  }
}
