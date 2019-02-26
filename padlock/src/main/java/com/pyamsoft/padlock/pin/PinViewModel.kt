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
import com.pyamsoft.padlock.model.pin.CreatePinEvent
import com.pyamsoft.pydroid.core.bus.Publisher
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class PinViewModel @Inject internal constructor(
  private val interactor: PinInteractor,
  private val createPinBus: Publisher<CreatePinEvent>
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
  fun onMasterPinCheckEvent(func: (Boolean) -> Unit): Disposable {
    return Disposables.empty()
//    return checkPinBus.listen()
//        .map { it.matching }
//        .subscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe(func)
  }
}
