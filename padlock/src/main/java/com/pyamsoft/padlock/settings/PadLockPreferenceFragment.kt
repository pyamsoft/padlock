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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.ClearPinPresenter
import com.pyamsoft.padlock.pin.PinDialog
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.requireView
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PadLockPreferenceFragment : AppSettingsPreferenceFragment(),
    SettingsPresenter.Callback,
    ClearAllPresenter.Callback,
    ClearDatabasePresenter.Callback,
    ClearPinPresenter.Callback {

  @field:Inject internal lateinit var clearDatabasePresenter: ClearDatabasePresenter
  @field:Inject internal lateinit var clearAllPresenter: ClearAllPresenter
  @field:Inject internal lateinit var clearPinPresenter: ClearPinPresenter
  @field:Inject internal lateinit var presenter: SettingsPresenter

  @field:Inject internal lateinit var settingsView: SettingsView

  override val preferenceXmlResId: Int = R.xml.preferences

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusSettingsComponent()
        .preferenceScreen(preferenceScreen)
        .build()
        .inject(this)

    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    settingsView.inflate(savedInstanceState)

    presenter.bind(viewLifecycleOwner, this)
    clearPinPresenter.bind(viewLifecycleOwner, this)
    clearDatabasePresenter.bind(viewLifecycleOwner, this)
    clearAllPresenter.bind(viewLifecycleOwner, this)
  }

  override fun onClearDatabaseRequest() {
    ConfirmDeleteAllDialog()
        .show(requireActivity(), "confirm_dialog")
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
    Snackbreak.bindTo(viewLifecycleOwner)
        .long(requireView(), "You must clear the current code before changing type")
        .setAction("Okay", DebouncedOnClickListener.create {
          PinDialog.newInstance(checkOnly = false, finishOnDismiss = false)
              .show(requireActivity(), PinDialog.TAG)
        })
        .show()
  }

  override fun onLockTypeSwitchSuccess(newType: String) {
    Timber.d("Change accepted, set value: $newType")
    settingsView.changeLockType(newType)
  }

  override fun onLockTypeSwitchError(throwable: Throwable) {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), throwable.localizedMessage)
        .show()
  }

  override fun onAllSettingsCleared() {
    Timber.d("Everything is cleared, kill self")
    val activityManager = requireNotNull(requireActivity().getSystemService<ActivityManager>())
    activityManager.clearApplicationUserData()
  }

  override fun onDatabaseCleared() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "Locked application database cleared")
        .show()
  }

  override fun onPinClearSuccess() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "You may now change lock type")
        .show()
  }

  override fun onPinClearFailed() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "Failed to clear master pin")
        .show()
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
