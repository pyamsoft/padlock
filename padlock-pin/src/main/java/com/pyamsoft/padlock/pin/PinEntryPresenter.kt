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

  override fun onBind(v: Callback) {
    super.onBind(v)
    checkMasterPinPresent(v::onMasterPinMissing, v::onMasterPinPresent)
  }

  private fun checkMasterPinPresent(onMasterPinMissing: () -> Unit,
      onMasterPinPresent: () -> Unit) {
    Timber.d("Check master pin present")
    dispose {
      interactor.hasMasterPin()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onMasterPinPresent()
            } else {
              onMasterPinMissing()
            }
          }, { Timber.e(it, "onError checkMasterPinPresent") })
    }
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
    dispose {
      interactor.submitPin(currentAttempt, reEntryAttempt, hint)
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
          })
    }
  }

  interface Callback {
    fun onMasterPinMissing()
    fun onMasterPinPresent()
  }

}
