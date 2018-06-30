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

package com.pyamsoft.padlock.pin

import com.pyamsoft.padlock.api.PinEntryInteractor
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.padlock.model.pin.PinEntryEvent.Clear
import com.pyamsoft.padlock.model.pin.PinEntryEvent.Create
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.presenter.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class PinEntryPresenter @Inject internal constructor(
  private val interactor: PinEntryInteractor,
  private val createPinBus: EventBus<CreatePinEvent>,
  private val clearPinBus: EventBus<ClearPinEvent>
) : Presenter<PinEntryPresenter.View>() {

  override fun onCreate() {
    super.onCreate()
    checkMasterPinPresent()
  }

  private fun checkMasterPinPresent() {
    Timber.d("Check master pin present")
    dispose {
      interactor.hasMasterPin()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            if (it) {
              view?.onMasterPinPresent()
            } else {
              view?.onMasterPinMissing()
            }
          }, { Timber.e(it, "onError checkMasterPinPresent") })
    }
  }

  private fun publish(event: CreatePinEvent) {
    createPinBus.publish(event)
  }

  private fun publish(event: ClearPinEvent) {
    clearPinBus.publish(event)
  }

  fun submit(
    currentAttempt: String,
    reEntryAttempt: String,
    hint: String
  ) {
    dispose {
      interactor.submitPin(currentAttempt, reEntryAttempt, hint)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doAfterTerminate { view?.onPinSubmitComplete() }
          .subscribe({
            when (it) {
              is Create -> {
                if (it.complete) {
                  publish(CreatePinEvent(true))
                  view?.onPinSubmitCreateSuccess()
                } else {
                  publish(CreatePinEvent(false))
                  view?.onPinSubmitCreateFailure()
                }
              }
              is Clear -> {
                if (it.complete) {
                  publish(ClearPinEvent(true))
                  view?.onPinSubmitClearSuccess()
                } else {
                  publish(ClearPinEvent(false))
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
