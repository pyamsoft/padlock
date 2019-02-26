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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.PinInteractor
import com.pyamsoft.padlock.pin.ConfirmPinPresenterImpl.CheckPinEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class ConfirmPinPresenterImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: PinInteractor,
  bus: EventBus<CheckPinEvent>
) : BasePresenter<CheckPinEvent, ConfirmPinPresenter.Callback>(bus),
    ConfirmPinPresenter {

  @CheckResult
  private fun checkPin(attempt: String): Single<Boolean> {
    enforcer.assertNotOnMainThread()
    return interactor.comparePin(attempt)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMapSingle { checkPin(it.attempt) }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { success ->
          if (success) {
            callback.onConfirmPinSuccess()
          } else {
            callback.onConfirmPinFailure()
          }
        }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun confirm(pin: String) {
    publish(CheckPinEvent(pin))
  }

  internal data class CheckPinEvent(val attempt: String)
}

