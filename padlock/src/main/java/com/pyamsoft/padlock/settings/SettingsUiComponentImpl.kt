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

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.ClearPinPresenter
import com.pyamsoft.padlock.pin.ConfirmPinPresenter
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import timber.log.Timber
import javax.inject.Inject

internal class SettingsUiComponentImpl @Inject internal constructor(
  private val clearDatabasePresenter: ClearDatabasePresenter,
  private val clearAllPresenter: ClearAllPresenter,
  private val clearPinPresenter: ClearPinPresenter,
  private val confirmPinPresenter: ConfirmPinPresenter,
  private val switchLockTypePresenter: SwitchLockTypePresenter,
  private val presenter: SettingsPresenter,
  private val settingsView: SettingsView
) : BaseUiComponent<SettingsUiComponent.Callback>(),
    SettingsUiComponent,
    SettingsPresenter.Callback,
    SwitchLockTypePresenter.Callback,
    ClearAllPresenter.Callback,
    ClearDatabasePresenter.Callback,
    ConfirmPinPresenter.Callback,
    ClearPinPresenter.Callback {

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: SettingsUiComponent.Callback
  ) {
    owner.doOnDestroy {
      settingsView.teardown()
      confirmPinPresenter.unbind()
      switchLockTypePresenter.unbind()
      clearAllPresenter.unbind()
      clearDatabasePresenter.unbind()
      clearDatabasePresenter.unbind()
      presenter.unbind()
    }

    settingsView.inflate(savedInstanceState)
    confirmPinPresenter.bind(this)
    switchLockTypePresenter.bind(this)
    clearAllPresenter.bind(this)
    clearDatabasePresenter.bind(this)
    clearDatabasePresenter.bind(this)
    presenter.bind(this)
  }

  override fun saveState(outState: Bundle) {
    settingsView.saveState(outState)
  }

  override fun onClearDatabaseRequest() {
    callback.showClearDatabaseConfirmationDialog()
  }

  override fun onSwitchLockTypeRequest(newType: String) {
    switchLockTypePresenter.switchLockType(newType)
  }

  override fun onLockTypeSwitchBlocked() {
    settingsView.promptChangeLockType {
      callback.showClearDatabaseConfirmationDialog()
    }
  }

  override fun onLockTypeSwitchSuccess(newType: String) {
    Timber.d("Change accepted, set value: $newType")
    settingsView.changeLockType(newType)
  }

  override fun onLockTypeSwitchError(throwable: Throwable) {
    settingsView.showMessage(throwable.message ?: "Failed to switch lock type")
  }

  override fun onAllSettingsCleared() {
    Timber.d("Everything is cleared, kill self")
    callback.onKillApplication()
  }

  override fun onDatabaseCleared() {
    settingsView.showMessage("Locked application database cleared")
  }

  override fun onPinClearSuccess() {
    settingsView.showMessage("You may now change lock type")
  }

  override fun onPinClearFailed() {
    settingsView.showMessage("Failed to clear master pin")
  }

  override fun onConfirmPinBegin() {
  }

  override fun onConfirmPinFailure(attempt: String) {
    settingsView.showMessage("Failed to clear master pin")
  }

  override fun onConfirmPinSuccess(attempt: String) {
    Timber.d("Clear old master pin")
    clearPinPresenter.clear(attempt)
  }

  override fun onConfirmPinComplete() {
  }

  override fun onClearAllSettingsError(throwable: Throwable) {
    settingsView.showMessage("Unable to reset application settings, please try again later.")
  }

  override fun onClearDatabaseError(throwable: Throwable) {
    settingsView.showMessage("Unable to reset database, please try again later.")
  }

}