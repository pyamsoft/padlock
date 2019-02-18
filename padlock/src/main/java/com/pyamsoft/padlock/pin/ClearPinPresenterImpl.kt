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

import com.pyamsoft.padlock.pin.ClearPinPresenter.Callback
import com.pyamsoft.padlock.pin.ClearPinPresenterImpl.ClearPinEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import com.pyamsoft.pydroid.ui.arch.destroy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class ClearPinPresenterImpl @Inject internal constructor(
  bus: EventBus<ClearPinEvent>
) : BasePresenter<ClearPinEvent, Callback>(bus),
    ClearPinPresenter {

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          if (it.success) {
            callback.onPinClearSuccess()
          } else {
            callback.onPinClearFailed()
          }
        }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun success() {
    clear(true)
  }

  override fun failure() {
    clear(false)
  }

  private fun clear(success: Boolean) {
    publish(ClearPinEvent(success))
  }

  internal data class ClearPinEvent(val success: Boolean)

}