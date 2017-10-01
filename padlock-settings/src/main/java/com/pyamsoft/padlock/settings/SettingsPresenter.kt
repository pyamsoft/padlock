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
import com.pyamsoft.padlock.service.ServiceFinishEvent
import com.pyamsoft.padlock.settings.ConfirmEvent.ALL
import com.pyamsoft.padlock.settings.ConfirmEvent.DATABASE
import com.pyamsoft.padlock.settings.SettingsPresenter.Callback
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
    private val receiver: ApplicationInstallReceiver,
    @Named("computation") computationScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onBind(v: Callback) {
    super.onBind(v)
    registerOnBus(v::onClearDatabase, v::onClearAll)
  }

  private fun registerOnBus(onClearDatabase: () -> Unit, onClearAll: () -> Unit) {
    dispose {
      bus.listen().flatMapSingle { type ->
        when (type) {
          DATABASE -> interactor.clearDatabase()
          ALL -> interactor.clearAll()
        }.map { type }
      }.subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              DATABASE -> onClearDatabase()
              ALL -> onClearAll()
              else -> throw IllegalArgumentException("Invalid enum: $it")
            }
          }, {
            Timber.e(it, "onError clear bus")
          })
    }
  }

  fun publishFinish() {
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

  fun checkLockType(onLockTypeChangeAccepted: () -> Unit,
      onLockTypeChangePrevented: () -> Unit, onLockTypeChangeError: (Throwable) -> Unit) {
    dispose {
      interactor.hasExistingMasterPassword()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onLockTypeChangePrevented()
            } else {
              onLockTypeChangeAccepted()
            }
          }, {
            Timber.e(it, "on error lock type change")
            onLockTypeChangeError(it)
          })
    }
  }

  interface Callback {

    fun onClearDatabase()

    fun onClearAll()
  }
}
