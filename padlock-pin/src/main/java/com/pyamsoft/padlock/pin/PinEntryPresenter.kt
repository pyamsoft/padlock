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

import com.pyamsoft.padlock.pin.PinEntryEvent.Clear
import com.pyamsoft.padlock.pin.PinEntryEvent.Create
import com.pyamsoft.padlock.pin.PinEntryPresenter.Callback
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class PinEntryPresenter @Inject internal constructor(private val interactor: PinEntryInteractor,
    private val createPinBus: EventBus<CreatePinEvent>,
    private val clearPinBus: EventBus<ClearPinEvent>,
    @Named("computation") computationScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler) : SchedulerPresenter<Callback>(computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    checkMasterPinPresent(bound::onMasterPinMissing, bound::onMasterPinPresent)
  }

  private fun checkMasterPinPresent(onMasterPinMissing: () -> Unit,
      onMasterPinPresent: () -> Unit) {
    disposeOnStop(interactor.hasMasterPin()
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({
          if (it) {
            onMasterPinPresent()
          } else {
            onMasterPinMissing()
          }
        }, { Timber.e(it, "onError checkMasterPinPresent") }))
  }

  fun publish(event: CreatePinEvent) {
    createPinBus.publish(event)
  }

  fun publish(event: ClearPinEvent) {
    clearPinBus.publish(event)
  }

  fun submit(currentAttempt: String, reEntryAttempt: String, hint: String,
      onCreateSuccess: () -> Unit, onCreateFailure: () -> Unit,
      onClearSuccess: () -> Unit, onClearFailure: () -> Unit,
      onSubmitError: (Throwable) -> Unit, onComplete: () -> Unit) {
    disposeOnStop(interactor.submitPin(currentAttempt, reEntryAttempt, hint)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .doAfterTerminate { onComplete() }
        .subscribe({
          when (it) {
            is Create -> {
              if (it.complete) {
                onCreateSuccess()
              } else {
                onCreateFailure()
              }
            }
            is Clear -> {
              if (it.complete) {
                onClearSuccess()
              } else {
                onClearFailure()
              }
            }
          }
        }, {
          Timber.e(it, "attemptPinSubmission onError")
          onSubmitError(it)
        }))
  }

  interface Callback {
    fun onMasterPinMissing()
    fun onMasterPinPresent()
  }

}
