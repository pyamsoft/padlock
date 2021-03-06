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

import com.pyamsoft.padlock.service.RecheckPresenter.Callback
import com.pyamsoft.padlock.service.RecheckPresenterImpl.RecheckEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class RecheckPresenterImpl @Inject internal constructor(
  bus: EventBus<RecheckEvent>
) : BasePresenter<RecheckEvent, Callback>(bus),
    RecheckPresenter {

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onRecheckRequired(it.packageName, it.className) }
        .destroy()
  }

  override fun onUnbind() {
  }

  override fun recheck(
    packageName: String,
    className: String
  ) {
    publish(RecheckEvent(packageName, className))
  }

  internal data class RecheckEvent(
    val packageName: String,
    val className: String
  )

}