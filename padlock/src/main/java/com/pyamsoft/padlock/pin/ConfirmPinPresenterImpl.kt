/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.pin

import com.pyamsoft.padlock.api.PinInteractor
import com.pyamsoft.padlock.pin.ConfirmPinPresenterImpl.CheckPinEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class ConfirmPinPresenterImpl @Inject internal constructor(
  private val interactor: PinInteractor,
  bus: EventBus<CheckPinEvent>
) : BasePresenter<CheckPinEvent, ConfirmPinPresenter.Callback>(bus),
    ConfirmPinPresenter {

  private var confirmDisposable by singleDisposable()

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { event ->
          val (attempt, success, checkOnly) = event
          if (success) {
            callback.onConfirmPinSuccess(attempt, checkOnly)
          } else {
            callback.onConfirmPinFailure(attempt, checkOnly)
          }
        }
        .destroy()
  }

  override fun onUnbind() {
    confirmDisposable.tryDispose()
  }

  override fun confirm(
    pin: String,
    checkOnly: Boolean
  ) {
    confirmDisposable = interactor.comparePin(pin)
        .map { pin to it }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ publish(CheckPinEvent(it.first, it.second, checkOnly)) }, {
          Timber.e(it, "Error confirming pin")
          publish(CheckPinEvent(pin, false, checkOnly))
        })
  }

  internal data class CheckPinEvent(
    val attempt: String,
    val success: Boolean,
    val checkOnly: Boolean
  )
}

