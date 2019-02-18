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
import com.pyamsoft.padlock.settings.SettingsPresenterImpl.SwitchLockTypeEvent
import com.pyamsoft.padlock.settings.SettingsPresenterImpl.SwitchLockTypeEvent.SwitchLockTypeBlocked
import com.pyamsoft.padlock.settings.SettingsPresenterImpl.SwitchLockTypeEvent.SwitchLockTypeError
import com.pyamsoft.padlock.settings.SettingsPresenterImpl.SwitchLockTypeEvent.SwitchLockTypeSuccess
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import com.pyamsoft.pydroid.ui.arch.destroy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@FragmentScope
internal class SettingsPresenterImpl @Inject internal constructor(
  private val interactor: SettingsInteractor,
  bus: EventBus<SwitchLockTypeEvent>
) : BasePresenter<SwitchLockTypeEvent, Callback>(bus),
    SettingsView.Callback,
    SettingsPresenter {

  private var installListenerDisposable by singleDisposable()
  private var lockTypeDisposable by singleDisposable()

  override fun onBind() {
    listenSwitchLockType()
  }

  override fun onUnbind() {
    lockTypeDisposable.tryDispose()
    installListenerDisposable.tryDispose()
  }

  override fun onSwitchLockTypeChanged(newType: String) {
    lockTypeDisposable = interactor.hasExistingMasterPassword()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ switchingBlocked ->
          if (switchingBlocked) {
            publish(SwitchLockTypeBlocked)
          } else {
            publish(SwitchLockTypeSuccess(newType))
          }
        }, {
          Timber.e(it, "Error switching lock type")
          publish(SwitchLockTypeError(it))
        })
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

  private fun listenSwitchLockType() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          return@subscribe when (it) {
            is SwitchLockTypeSuccess -> callback.onLockTypeSwitchSuccess(it.newType)
            is SwitchLockTypeBlocked -> callback.onLockTypeSwitchBlocked()
            is SwitchLockTypeError -> callback.onLockTypeSwitchError(it.error)
          }
        }
        .destroy(owner)
  }

  internal sealed class SwitchLockTypeEvent {

    data class SwitchLockTypeSuccess(val newType: String) : SwitchLockTypeEvent()

    object SwitchLockTypeBlocked : SwitchLockTypeEvent()

    data class SwitchLockTypeError(val error: Throwable) : SwitchLockTypeEvent()

  }

}
