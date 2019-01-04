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
import com.pyamsoft.padlock.model.pin.CheckPinEvent
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.padlock.model.pin.PinEntryEvent.Clear
import com.pyamsoft.padlock.model.pin.PinEntryEvent.Create
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Publisher
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class PinViewModel @Inject internal constructor(
  private val interactor: PinInteractor,
  private val createPinBus: Publisher<CreatePinEvent>,
  private val clearPinBus: Publisher<ClearPinEvent>,
  private val checkPinBus: EventBus<CheckPinEvent>
) {

  @CheckResult
  fun checkMasterPin(
    onMasterPinPresent: () -> Unit,
    onMasterPinMissing: () -> Unit
  ): Disposable {
    return interactor.hasMasterPin()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Consumer {
          if (it) {
            onMasterPinPresent()
          } else {
            onMasterPinMissing()
          }
        })
  }

  @CheckResult
  fun submit(
    currentAttempt: String,
    reEntryAttempt: String,
    hint: String,
    onSubmitComplete: () -> Unit
  ): Disposable {
    return interactor.submitPin(currentAttempt, reEntryAttempt, hint)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { onSubmitComplete() }
        .subscribe(Consumer {
          when (it) {
            is Create -> createPinBus.publish(CreatePinEvent(it.complete))
            is Clear -> clearPinBus.publish(ClearPinEvent(it.complete))
          }
        })
  }

  @CheckResult
  fun onMasterPinCheckEvent(func: (Boolean) -> Unit): Disposable {
    return checkPinBus.listen()
        .map { it.matching }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
  }

  @CheckResult
  fun checkPin(attempt: String): Disposable {
    return interactor.comparePin(attempt)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ checkPinBus.publish(CheckPinEvent(it)) }, {
          Timber.e(it, "Error checking pin and attempt")
          checkPinBus.publish(CheckPinEvent(false))
        })
  }
}
