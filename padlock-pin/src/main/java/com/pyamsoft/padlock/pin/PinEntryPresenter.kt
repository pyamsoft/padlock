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
import com.pyamsoft.padlock.pin.PinEntryPresenter.View
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
        @Named("main") mainScheduler: Scheduler) : SchedulerPresenter<View>(computationScheduler,
        ioScheduler, mainScheduler) {

    override fun onBind(v: View) {
        super.onBind(v)
        checkMasterPinPresent(v)
    }

    private fun checkMasterPinPresent(v: MasterPinCallback) {
        Timber.d("Check master pin present")
        dispose {
            interactor.hasMasterPin()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        if (it) {
                            v.onMasterPinPresent()
                        } else {
                            v.onMasterPinMissing()
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

    fun submit(currentAttempt: String, reEntryAttempt: String, hint: String) {
        dispose {
            interactor.submitPin(currentAttempt, reEntryAttempt, hint)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .doAfterTerminate { view?.onPinSubmitComplete() }
                    .subscribe({
                        when (it) {
                            is Create -> {
                                if (it.complete) {
                                    view?.onPinSubmitCreateSuccess()
                                } else {
                                    view?.onPinSubmitCreateFailure()
                                }
                            }
                            is Clear -> {
                                if (it.complete) {
                                    view?.onPinSubmitClearSuccess()
                                } else {
                                    view?.onPinSubmitClearFailure()
                                }
                            }
                        }
                    }, {
                        Timber.e(it, "attemptPinSubmission onError")
                        view?.onPinSubmitError(it)
                    })
        }
    }

    interface View : MasterPinCallback, SubmitCallback

    interface SubmitCallback {
        fun onPinSubmitCreateSuccess()
        fun onPinSubmitCreateFailure()
        fun onPinSubmitClearSuccess()
        fun onPinSubmitClearFailure()
        fun onPinSubmitError(throwable: Throwable)
        fun onPinSubmitComplete()
    }

    interface MasterPinCallback {
        fun onMasterPinMissing()
        fun onMasterPinPresent()
    }

}
