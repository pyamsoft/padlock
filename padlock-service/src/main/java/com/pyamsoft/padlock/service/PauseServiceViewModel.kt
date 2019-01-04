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
import com.pyamsoft.padlock.model.pin.CheckPinEvent
import com.pyamsoft.pydroid.core.bus.Listener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Named

class PauseServiceViewModel @Inject internal constructor(
  private val checkPinBus: Listener<CheckPinEvent>,
  @param:Named("recreate_listener") private val recreateListener: Listener<Unit>
) {

  @CheckResult
  fun onCheckPinEventSuccess(func: () -> Unit): Disposable {
    return checkPinBus.listen()
        .filter { it.matching }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onRecreateEvent(func: () -> Unit): Disposable {
    return recreateListener.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onCheckPinEventFailed(func: () -> Unit): Disposable {
    return checkPinBus.listen()
        .filter { !it.matching }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

}
