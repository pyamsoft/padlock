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

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.PinInteractor
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.padlock.model.pin.PinEntryEvent
import com.pyamsoft.padlock.model.pin.PinEntryEvent.Clear
import com.pyamsoft.padlock.model.pin.PinEntryEvent.Create
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper.Complete
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper.Error
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper.Success
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class PinViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val interactor: PinInteractor,
  private val createPinBus: Publisher<CreatePinEvent>,
  private val clearPinBus: Publisher<ClearPinEvent>,
  private val checkPinBus: EventBus<CheckPinEvent>
) : BaseViewModel(owner) {

  private var masterPinDisposable by disposable()
  private var pinEntryDisposable by disposable()

  private val masterPinPresentBus = DataBus<Boolean>()
  private val pinEntryBus = DataBus<PinEntryEvent>()

  override fun onCleared() {
    super.onCleared()
    masterPinDisposable.tryDispose()
    pinEntryDisposable.tryDispose()
  }

  @CheckResult
  private fun masterPinPresenceBus(): Observable<Boolean> {
    return masterPinPresentBus.listen()
        .filter { it is Success }
        .map { it as Success }
        .map { it.data }
  }

  fun onMasterPinPresent(func: () -> Unit) {
    dispose {
      masterPinPresenceBus().filter { it }
          .map { Unit }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onMasterPinMissing(func: () -> Unit) {
    dispose {
      masterPinPresenceBus()
          .filter { !it }
          .map { Unit }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun checkMasterPin() {
    masterPinDisposable = interactor.hasMasterPin()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ masterPinPresentBus.publishSuccess(it) }, {
          Timber.e(it, "Error checkMasterPinPresent")
          masterPinPresentBus.publishError(it)
        })
  }

  fun onMasterPinSubmitError(func: (Throwable) -> Unit) {
    dispose {
      pinEntryBus.listen()
          .filter { it is Error }
          .map { it as Error }
          .map { it.error }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func(it) }
    }
  }

  fun onMasterPinSubmitted(func: () -> Unit) {
    dispose {
      pinEntryBus.listen()
          .filter { it is Complete }
          .map { it as Complete }
          .map { Unit }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun submit(
    currentAttempt: String,
    reEntryAttempt: String,
    hint: String
  ) {
    pinEntryDisposable = interactor.submitPin(currentAttempt, reEntryAttempt, hint)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { pinEntryBus.publishLoading(false) }
        .doAfterTerminate { pinEntryBus.publishComplete() }
        .subscribe({
          when (it) {
            is Create -> createPinBus.publish(CreatePinEvent(it.complete))
            is Clear -> clearPinBus.publish(ClearPinEvent(it.complete))
          }
          pinEntryBus.publishSuccess(it)
        }, {
          Timber.e(it, "Pin entry submission error")
          pinEntryBus.publishError(it)
        })
  }

  fun onMasterPinCheckEvent(func: (Boolean) -> Unit) {
    dispose {
      checkPinBus.listen()
          .map { it.matching }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun checkPin(attempt: String) {
    pinEntryDisposable = interactor.comparePin(attempt)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ checkPinBus.publish(CheckPinEvent(it)) }, {
          Timber.e(it, "Error checking pin and attempt")
          checkPinBus.publish(CheckPinEvent(false))
        })
  }
}
