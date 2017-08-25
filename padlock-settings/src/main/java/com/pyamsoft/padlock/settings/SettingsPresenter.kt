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
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback, Unit>(computationScheduler,
    ioScheduler, mainScheduler) {

  override fun onCreate(bound: Callback) {
    super.onCreate(bound)
    registerOnBus(bound::onClearDatabase, bound::onClearAll)
  }

  private fun registerOnBus(onClearDatabase: () -> Unit, onClearAll: () -> Unit) {
    disposeOnDestroy {
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
    disposeOnStop {
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
    disposeOnStop {
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
