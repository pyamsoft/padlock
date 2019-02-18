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

package com.pyamsoft.padlock.service

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.model.service.ServicePauseState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class LockServiceViewModel @Inject internal constructor(
  private val interactor: LockServiceInteractor
) {

  init {
    interactor.init()
  }

  @CheckResult
  fun observeScreenState(
    onScreenOn: () -> Unit,
    onScreenOff: () -> Unit
  ): Disposable {
    return interactor.observeScreenState()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { screenOn: Boolean ->
          if (screenOn) {
            onScreenOn()
          } else {
            onScreenOff()
          }
        }
  }

  fun setServicePaused(paused: ServicePauseState) {
    interactor.setPauseState(paused)
  }

}
