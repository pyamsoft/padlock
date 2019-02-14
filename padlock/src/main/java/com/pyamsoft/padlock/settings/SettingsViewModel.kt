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

package com.pyamsoft.padlock.settings

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel @Inject internal constructor(
  private val interactor: SettingsInteractor,
  private val clearPinBus: Listener<ClearPinEvent>,
  @param:Named("recreate_publisher") private val recreatePublisher: Publisher<Unit>
) {

  @CheckResult
  fun onPinClearSuccess(func: () -> Unit): Disposable {
    return clearPinBus.listen()
        .filter { it.success }
        .map { Unit }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onPinClearFailed(func: () -> Unit): Disposable {
    return clearPinBus.listen()
        .filter { !it.success }
        .map { Unit }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun updateApplicationReceiver(
    onUpdateBegin: () -> Unit,
    onUpdateSuccess: () -> Unit,
    onUpdateError: (error: Throwable) -> Unit,
    onUpdateComplete: () -> Unit
  ): Disposable {
    return interactor.updateApplicationReceiver()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { onUpdateBegin() }
        .doAfterTerminate { onUpdateComplete() }
        .subscribe({ onUpdateSuccess() }, {
          Timber.e(it, "Error updating application receiver")
          onUpdateError(it)
        })
  }

  fun publishRecreate() {
    Timber.d("Publish recreate event")
    recreatePublisher.publish(Unit)
  }
}
