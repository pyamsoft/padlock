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

package com.pyamsoft.padlock.settings

import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.model.ConfirmEvent.ALL
import com.pyamsoft.padlock.model.ConfirmEvent.DATABASE
import com.pyamsoft.padlock.model.service.ServiceFinishEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.presenter.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SettingsPresenter @Inject internal constructor(
  private val interactor: SettingsInteractor,
  private val bus: EventBus<ConfirmEvent>,
  private val serviceFinishBus: EventBus<ServiceFinishEvent>,
  private val clearPinBus: EventBus<ClearPinEvent>,
  private val receiver: ApplicationInstallReceiver
) : Presenter<SettingsPresenter.View>() {

  override fun onCreate() {
    super.onCreate()
    registerOnBus()
    registerOnClearBus()
  }

  private fun registerOnBus() {
    dispose {
      bus.listen()
          .flatMapSingle { type ->
            when (type) {
              DATABASE -> interactor.clearDatabase()
              ALL -> interactor.clearAll()
            }.map { type }
          }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
      clearPinBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
