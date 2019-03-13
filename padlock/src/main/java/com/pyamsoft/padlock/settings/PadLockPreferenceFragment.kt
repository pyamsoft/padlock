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
import com.pyamsoft.padlock.pin.PinConfirmDialog
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

class PadLockPreferenceFragment : AppSettingsPreferenceFragment(),
    SettingsUiComponent.Callback {

  @field:Inject internal lateinit var component: SettingsUiComponent
  @field:Inject internal lateinit var toolbarView: SettingsToolbarView

  override val preferenceXmlResId: Int = R.xml.preferences

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusSettingsComponent()
        .toolbarActivity(requireToolbarActivity())
        .owner(viewLifecycleOwner)
        .view(view)
        .preferenceScreen(preferenceScreen)
        .build()
        .inject(this)

    toolbarView.inflate(savedInstanceState)
    component.bind(viewLifecycleOwner, savedInstanceState, this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    toolbarView.teardown()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    toolbarView.saveState(outState)
    component.saveState(outState)
  }

  override fun onClearAllClicked() {
    super.onClearAllClicked()
    ConfirmDeleteAllDialog()
        .show(requireActivity(), "confirm_dialog")
  }

  override fun showClearDatabaseConfirmationDialog() {
    ConfirmDeleteDatabaseDialog().show(requireActivity(), "confirm_dialog")
  }

  override fun onKillApplication() {
    val activityManager = requireNotNull(requireActivity().getSystemService<ActivityManager>())
    activityManager.clearApplicationUserData()
  }

  override fun onRequestPinChange() {
    PinConfirmDialog.newInstance(finishOnDismiss = false)
        .show(requireActivity(), PinConfirmDialog.TAG)
  }

  companion object {

    const val TAG = "PadLockPreferenceFragment"
  }
}
