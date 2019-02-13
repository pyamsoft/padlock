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
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.pin.PinDialog
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.requireView
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PadLockPreferenceFragment : AppSettingsPreferenceFragment(), LockTypePresenter.Callback {

  @field:Inject internal lateinit var lockTypePresenter: LockTypePresenter
  @field:Inject internal lateinit var viewModel: SettingsViewModel
  @field:Inject internal lateinit var settingsView: SettingsView

  override val preferenceXmlResId: Int = R.xml.preferences

  private var installReceiverDisposable by singleDisposable()
  private var allClearDisposable by singleDisposable()
  private var dbClearDisposable by singleDisposable()
  private var pinClearFailedDisposable by singleDisposable()
  private var pinClearSuccessDisposable by singleDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusSettingsComponent()
        .preferenceScreen(preferenceScreen)
        .owner(viewLifecycleOwner)
        .build()
        .inject(this)

    settingsView.create()
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    settingsView.onClearDatabaseClicked {
      ConfirmationDialog.newInstance(ConfirmEvent.DATABASE)
          .show(requireActivity(), "confirm_dialog")
    }

    settingsView.onInstallListenerClicked {
      installReceiverDisposable = viewModel.updateApplicationReceiver(
          onUpdateBegin = {},
          onUpdateSuccess = { Timber.d("Updated application install receiver") },
          onUpdateError = { Timber.e(it, "Failed to update application install receiver") },
          onUpdateComplete = {}
      )
    }

    settingsView.onLockTypeChangeAttempt {
      lockTypePresenter.switchType(it)
    }

    allClearDisposable = viewModel.onAllSettingsCleared { onClearAll() }
    dbClearDisposable = viewModel.onDatabaseCleared { onClearDatabase() }
    pinClearFailedDisposable = viewModel.onPinClearFailed { onMasterPinClearFailure() }
    pinClearSuccessDisposable = viewModel.onPinClearSuccess { onMasterPinClearSuccess() }

    lockTypePresenter.bind(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    installReceiverDisposable.tryDispose()
    allClearDisposable.tryDispose()
    dbClearDisposable.tryDispose()
    pinClearFailedDisposable.tryDispose()
    pinClearSuccessDisposable.tryDispose()
  }

  override fun onDarkThemeClicked(dark: Boolean) {
    super.onDarkThemeClicked(dark)

    // Publish incase any other activities are listening
    viewModel.publishRecreate()
  }

  override fun onClearAllClicked() {
    super.onClearAllClicked()
    ConfirmationDialog.newInstance(ConfirmEvent.ALL)
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

  private fun onClearDatabase() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "Locked application database cleared")
        .show()
  }

  private fun onMasterPinClearFailure() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "Failed to clear master pin")
        .show()
  }

  private fun onMasterPinClearSuccess() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "You may now change lock type")
        .show()
  }

  private fun onClearAll() {
    Timber.d("Everything is cleared, kill self")
    val activityManager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.clearApplicationUserData()
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
