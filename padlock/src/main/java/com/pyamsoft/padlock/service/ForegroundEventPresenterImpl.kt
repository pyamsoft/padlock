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

import com.pyamsoft.padlock.api.service.LockServiceInteractor.ForegroundEvent
import com.pyamsoft.padlock.service.ForegroundEventPresenter.Callback
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class ForegroundEventPresenterImpl @Inject internal constructor(
  bus: EventBus<ForegroundEvent>
) : BasePresenter<ForegroundEvent, Callback>(bus),
    ForegroundEventPresenter {

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onForegroundEvent(it.packageName, it.className) }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun foreground(
    packageName: String,
    className: String
  ) {
    publish(ForegroundEvent(packageName, className))
  }

}