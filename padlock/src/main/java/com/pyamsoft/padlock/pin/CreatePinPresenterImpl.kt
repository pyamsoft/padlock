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
import com.pyamsoft.padlock.pin.CreatePinPresenterImpl.CreatePinEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class CreatePinPresenterImpl @Inject internal constructor(
  private val interactor: PinInteractor,
  bus: EventBus<CreatePinEvent>
) : BasePresenter<CreatePinEvent, CreatePinPresenter.Callback>(bus),
    CreatePinPresenter {

  private var createPinDisposable by singleDisposable()

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          if (it.success) {
            callback.onCreatePinSuccess()
          } else {
            callback.onCreatePinFailure()
          }
        }
        .destroy(owner)
  }

  override fun onUnbind() {
    createPinDisposable.tryDispose()
  }

  override fun create(
    attempt: String,
    reEntry: String,
    hint: String
  ) {
    createPinDisposable = interactor.createPin(attempt, reEntry, hint)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { callback.onCreatePinBegin() }
        .doAfterTerminate { callback.onCreatePinComplete() }
        .subscribe({ publish(CreatePinEvent(it)) }, {
          Timber.e(it, "Error creating PIN")
          callback.onCreatePinFailure()
        })
  }

  internal data class CreatePinEvent(val success: Boolean)
}
