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

import android.app.ActivityManager
import android.os.Bundle
import android.view.View
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.ClearPinPresenter
import com.pyamsoft.padlock.pin.ConfirmPinPresenter
import com.pyamsoft.padlock.pin.PinConfirmDialog
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PadLockPreferenceFragment : AppSettingsPreferenceFragment(),
    SettingsPresenter.Callback,
    SwitchLockTypePresenter.Callback,
    ClearAllPresenter.Callback,
    ClearDatabasePresenter.Callback,
    ConfirmPinPresenter.Callback,
    ClearPinPresenter.Callback {

  @field:Inject internal lateinit var clearDatabasePresenter: ClearDatabasePresenter
  @field:Inject internal lateinit var clearAllPresenter: ClearAllPresenter
  @field:Inject internal lateinit var clearPinPresenter: ClearPinPresenter
  @field:Inject internal lateinit var confirmPinPresenter: ConfirmPinPresenter
  @field:Inject internal lateinit var switchLockTypePresenter: SwitchLockTypePresenter
  @field:Inject internal lateinit var presenter: SettingsPresenter

  @field:Inject internal lateinit var settingsView: SettingsView

  override val preferenceXmlResId: Int = R.xml.preferences

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusSettingsComponent()
        .owner(viewLifecycleOwner)
        .view(view)
        .preferenceScreen(preferenceScreen)
        .build()
        .inject(this)

    settingsView.inflate(savedInstanceState)

    confirmPinPresenter.bind(viewLifecycleOwner, this)
    switchLockTypePresenter.bind(viewLifecycleOwner, this)
    clearPinPresenter.bind(viewLifecycleOwner, this)
    clearDatabasePresenter.bind(viewLifecycleOwner, this)
    clearAllPresenter.bind(viewLifecycleOwner, this)
    presenter.bind(viewLifecycleOwner, this)
  }

  override fun onClearDatabaseRequest() {
    ConfirmDeleteAllDialog()
        .show(requireActivity(), "confirm_dialog")
  }

  override fun onSwitchLockTypeRequest(newType: String) {
    switchLockTypePresenter.switchLockType(newType)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    settingsView.teardown()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    settingsView.saveState(outState)
  }

  override fun onClearAllClicked() {
    super.onClearAllClicked()
    ConfirmDeleteAllDialog()
        .show(requireActivity(), "confirm_dialog")
  }

  override fun onLockTypeSwitchBlocked() {
    settingsView.promptChangeLockType {
      PinConfirmDialog.newInstance(finishOnDismiss = false)
          .show(requireActivity(), PinConfirmDialog.TAG)
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
    val activityManager = requireNotNull(requireActivity().getSystemService<ActivityManager>())
    activityManager.clearApplicationUserData()
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
    Timber.d("Clear old master pin")
    clearPinPresenter.clear(attempt)
  }

  override fun onConfirmPinSuccess(attempt: String) {
    settingsView.showMessage("Failed to clear master pin")
  }

  override fun onConfirmPinComplete() {
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  companion object {

    const val TAG = "PadLockPreferenceFragment"
  }
}
