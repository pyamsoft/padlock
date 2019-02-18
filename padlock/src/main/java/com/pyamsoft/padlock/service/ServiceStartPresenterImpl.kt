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

import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.ENABLED
import com.pyamsoft.padlock.model.service.ServicePauseState
import com.pyamsoft.padlock.service.ServiceStartPresenter.Callback
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import com.pyamsoft.pydroid.ui.arch.destroy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class ServiceStartPresenterImpl @Inject internal constructor(
  private val interactor: LockServiceInteractor
) : BasePresenter<Unit, Callback>(RxBus.empty()),
    ServiceStartPresenter {

  override fun onBind() {
    interactor.observeServiceState()
        .filter { it == ENABLED }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onServiceStarted() }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun start() {
    interactor.setPauseState(ServicePauseState.STARTED)
  }
}