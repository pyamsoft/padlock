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
import com.pyamsoft.padlock.settings.SwitchLockTypePresenterImpl.SwitchLockTypeEvent
import com.pyamsoft.padlock.settings.SwitchLockTypePresenterImpl.SwitchLockTypeEvent.SwitchLockTypeBlocked
import com.pyamsoft.padlock.settings.SwitchLockTypePresenterImpl.SwitchLockTypeEvent.SwitchLockTypeError
import com.pyamsoft.padlock.settings.SwitchLockTypePresenterImpl.SwitchLockTypeEvent.SwitchLockTypeSuccess
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class SwitchLockTypePresenterImpl @Inject internal constructor(
  private val interactor: SettingsInteractor,
  bus: EventBus<SwitchLockTypeEvent>
) : BasePresenter<SwitchLockTypeEvent, SwitchLockTypePresenter.Callback>(bus),
    SwitchLockTypePresenter {

  private var lockTypeDisposable by singleDisposable()

  override fun onBind() {
    listen().subscribe {
      return@subscribe when (it) {
        is SwitchLockTypeSuccess -> callback.onLockTypeSwitchSuccess(it.newType)
        is SwitchLockTypeBlocked -> callback.onLockTypeSwitchBlocked()
        is SwitchLockTypeError -> callback.onLockTypeSwitchError(it.error)
      }
    }
        .destroy(owner)
  }

  override fun onUnbind() {
    lockTypeDisposable.tryDispose()
  }

  override fun switchLockType(newType: String) {
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

  internal sealed class SwitchLockTypeEvent {

    data class SwitchLockTypeSuccess(val newType: String) : SwitchLockTypeEvent()

    object SwitchLockTypeBlocked : SwitchLockTypeEvent()

    data class SwitchLockTypeError(val error: Throwable) : SwitchLockTypeEvent()

  }

}
