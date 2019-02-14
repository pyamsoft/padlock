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

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.settings.LockTypeSwitchEvent.LockTypeSwitchBlocked
import com.pyamsoft.padlock.settings.LockTypeSwitchEvent.LockTypeSwitchError
import com.pyamsoft.padlock.settings.LockTypeSwitchEvent.LockTypeSwitchSuccess
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.arch.BasePresenter
import com.pyamsoft.pydroid.ui.arch.destroy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SwitchLockTypePresenterImpl @Inject internal constructor(
  private val interactor: SettingsInteractor,
  owner: LifecycleOwner,
  bus: EventBus<LockTypeSwitchEvent>
) : BasePresenter<LockTypeSwitchEvent, SwitchLockTypePresenter.Callback>(owner, bus), SwitchLockTypePresenter {

  private var lockTypeDisposable by singleDisposable()

  override fun onBind() {
    listen().subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          return@subscribe when (it) {
            is LockTypeSwitchSuccess -> callback.onLockTypeSwitchSuccess(it.newType)
            is LockTypeSwitchBlocked -> callback.onLockTypeSwitchBlocked()
            is LockTypeSwitchError -> callback.onLockTypeSwitchError(it.error)
          }
        }
        .destroy(owner)
  }

  override fun onUnbind() {
    lockTypeDisposable.tryDispose()
  }

  override fun switchType(newType: String) {
    lockTypeDisposable = interactor.hasExistingMasterPassword()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ switchingBlocked ->
          if (switchingBlocked) {
            publish(LockTypeSwitchBlocked)
          } else {
            publish(LockTypeSwitchSuccess(newType))
          }
        }, {
          Timber.e(it, "Error switching lock type")
          publish(LockTypeSwitchError(it))
        })
  }
}
