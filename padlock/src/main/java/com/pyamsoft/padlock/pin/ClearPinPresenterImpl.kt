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
import com.pyamsoft.padlock.pin.ClearPinPresenter.Callback
import com.pyamsoft.padlock.pin.ClearPinPresenterImpl.ClearPinEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class ClearPinPresenterImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: PinInteractor,
  bus: EventBus<ClearPinEvent>
) : BasePresenter<ClearPinEvent, Callback>(bus),
    ClearPinPresenter {

  @CheckResult
  private fun clearPin(attempt: String): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()

      return@defer interactor.clearPin(attempt)
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.io())
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }

  override fun onBind() {
    listen().flatMapSingle { clearPin(it.attempt) }
        .subscribeOn(Schedulers.trampoline())
        .observeOn(Schedulers.trampoline())
        .subscribe { success ->
          if (success) {
            callback.onPinClearSuccess()
          } else {
            callback.onPinClearFailed()
          }
        }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun clear(attempt: String) {
    publish(ClearPinEvent(attempt))
  }

  internal data class ClearPinEvent(val attempt: String)

}