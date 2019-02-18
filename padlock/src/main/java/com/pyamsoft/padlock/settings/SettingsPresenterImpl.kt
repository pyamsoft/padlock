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

import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.padlock.settings.SettingsPresenter.Callback
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@FragmentScope
internal class SettingsPresenterImpl @Inject internal constructor(
  private val interactor: SettingsInteractor
) : BasePresenter<Unit, Callback>(RxBus.empty()),
    SettingsView.Callback,
    SettingsPresenter {

  private var installListenerDisposable by singleDisposable()

  override fun onBind() {
  }

  override fun onUnbind() {
    installListenerDisposable.tryDispose()
  }

  override fun onSwitchLockTypeChanged(newType: String) {
    callback.onSwitchLockTypeRequest(newType)
  }

  override fun onInstallListenerClicked() {
    installListenerDisposable = interactor.updateApplicationReceiver()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ Timber.d("Application install listener state updated") }, {
          Timber.e(it, "Error updating application receiver")
        })
  }

  override fun onClearDatabaseClicked() {
    callback.onClearDatabaseRequest()
  }

}
