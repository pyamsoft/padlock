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

package com.pyamsoft.padlock.settings

import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.pin.ClearPinEvent
import com.pyamsoft.padlock.service.ServiceFinishEvent
import com.pyamsoft.padlock.settings.ConfirmEvent.ALL
import com.pyamsoft.padlock.settings.ConfirmEvent.DATABASE
import com.pyamsoft.padlock.settings.SettingsPresenter.View
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SettingsPresenter @Inject internal constructor(
        private val interactor: SettingsInteractor,
        private val bus: EventBus<ConfirmEvent>,
        private val serviceFinishBus: EventBus<ServiceFinishEvent>,
        private val clearPinBus: EventBus<ClearPinEvent>,
        private val receiver: ApplicationInstallReceiver,
        @Named("computation") computationScheduler: Scheduler,
        @Named("main") mainScheduler: Scheduler,
        @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<View>(computationScheduler,
        ioScheduler, mainScheduler) {

    override fun onCreate() {
        super.onCreate()
        registerOnBus()
        registerOnClearBus()
    }

    private fun registerOnBus() {
        dispose {
            bus.listen().flatMapSingle { type ->
                when (type) {
                    DATABASE -> interactor.clearDatabase()
                    ALL -> interactor.clearAll()
                }.map { type }
            }.subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({
                        when (it) {
                            DATABASE -> view?.onClearDatabase()
                            ALL -> {
                                publishFinish()
                                view?.onClearAll()
                            }
                            else -> throw IllegalArgumentException("Invalid enum: $it")
                        }
                    }, {
                        Timber.e(it, "onError clear bus")
                    })
        }
    }

    private fun registerOnClearBus() {
        dispose {
            clearPinBus.listen().subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
                    .subscribe({
                        if (it.success) {
                            view?.onMasterPinClearSuccess()
                        } else {
                            view?.onMasterPinClearFailure()
                        }
                    }, {
                        Timber.e(it, "error clear pin bus")
                    })
        }
    }

    private fun publishFinish() {
        serviceFinishBus.publish(ServiceFinishEvent)
    }

    fun setApplicationInstallReceiverState() {
        dispose {
            interactor.isInstallListenerEnabled()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        if (it) {
                            receiver.register()
                        } else {
                            receiver.unregister()
                        }
                    }, { Timber.e(it, "onError setApplicationInstallReceiverState") })
        }
    }

    fun checkLockType(value: String) {
        dispose {
            interactor.hasExistingMasterPassword()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        if (it) {
                            view?.onLockTypeChangePrevented()
                        } else {
                            view?.onLockTypeChangeAccepted(value)
                        }
                    }, {
                        Timber.e(it, "on error lock type change")
                        view?.onLockTypeChangeError(it)
                    })
        }
    }

    interface View : ClearCallback, LockTypeChangeCallback, MasterPinClearCallback

    interface LockTypeChangeCallback {

        fun onLockTypeChangePrevented()

        fun onLockTypeChangeAccepted(value: String)

        fun onLockTypeChangeError(throwable: Throwable)
    }

    interface MasterPinClearCallback {

        fun onMasterPinClearSuccess()

        fun onMasterPinClearFailure()
    }

    interface ClearCallback {

        fun onClearDatabase()

        fun onClearAll()
    }
}
