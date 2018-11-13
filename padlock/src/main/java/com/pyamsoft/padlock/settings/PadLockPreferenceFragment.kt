/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.pin.PinDialog
import com.pyamsoft.pydroid.ui.app.fragment.SettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.fragment.requireView
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PadLockPreferenceFragment : SettingsPreferenceFragment() {

  @field:Inject internal lateinit var viewModel: SettingsViewModel
  @field:Inject internal lateinit var theming: Theming

  override val preferenceXmlResId: Int = R.xml.preferences

  override val rootViewContainer: Int = R.id.fragment_container

  override val applicationName: String
    get() = getString(R.string.app_name)

  override val bugreportUrl: String = "https://github.com/pyamsoft/padlock/issues"

  private lateinit var lockType: ListPreference

  override fun onClearAllClicked() {
    ConfirmationDialog.newInstance(ConfirmEvent.ALL)
        .show(requireActivity(), "confirm_dialog")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusSettingsComponent(SettingsModule(viewLifecycleOwner))
        .inject(this)
    return requireNotNull(super.onCreateView(inflater, container, savedInstanceState))
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupClearPreference()
    setupInstallListenerPreference()
    setupLockTypePreference()
    setupThemePreference()

    viewModel.onAllSettingsCleared { onClearAll() }
    viewModel.onDatabaseCleared { onClearDatabase() }
    viewModel.onPinClearFailed { onMasterPinClearFailure() }
    viewModel.onPinCleared { onMasterPinClearSuccess() }
    viewModel.onLockTypeSwitched { wrapper ->
      wrapper.onSuccess {
        if (it.isEmpty()) {
          onLockTypeChangePrevented()
        } else {
          onLockTypeChangeAccepted(it)
        }
      }
      wrapper.onError { onLockTypeChangeError(it) }
    }

    viewModel.onApplicationReceiverChanged { wrapper ->
      wrapper.onSuccess { Timber.d("Application notifier status changed") }
      wrapper.onError { Timber.e(it, "Application notified status error") }
    }
  }

  private fun setupThemePreference() {
    val darkMode = findPreference(getString(R.string.dark_mode_key))
    darkMode.setOnPreferenceChangeListener { _, newValue ->
      if (newValue is Boolean) {
        // Set dark mode
        theming.setDarkTheme(newValue)

        // Publish incase any other activities are listening
        viewModel.publishRecreate()

        // Recreate self
        requireActivity().recreate()
        return@setOnPreferenceChangeListener true
      } else {
        return@setOnPreferenceChangeListener false
      }
    }
  }

  private fun setupLockTypePreference() {
    lockType = findPreference(getString(R.string.lock_screen_type_key)) as ListPreference
    lockType.setOnPreferenceChangeListener { _, value ->
      if (value is String) {
        viewModel.switchLockType(value)
        // Always return false here, the callback will decide if we can set value properly
        return@setOnPreferenceChangeListener false
      } else {
        return@setOnPreferenceChangeListener false
      }
    }
  }

  private fun setupInstallListenerPreference() {
    val installListener = findPreference(getString(R.string.install_listener_key))
    installListener.setOnPreferenceClickListener {
      viewModel.updateApplicationReceiver()
      return@setOnPreferenceClickListener true
    }
  }

  private fun setupClearPreference() {
    val clearDb = findPreference(getString(R.string.clear_db_key))
    clearDb.setOnPreferenceClickListener {
      Timber.d("Clear DB onClick")
      ConfirmationDialog.newInstance(ConfirmEvent.DATABASE)
          .show(requireActivity(), "confirm_dialog")
      return@setOnPreferenceClickListener true
    }
  }

  private fun onLockTypeChangeAccepted(value: String) {
    Timber.d("Change accepted, set value: %s", value)
    lockType.value = value
  }

  private fun onLockTypeChangePrevented() {
    Snackbreak.long(requireView(), "You must clear the current code before changing type")
        .setAction("Okay", DebouncedOnClickListener.create {
          PinDialog.newInstance(checkOnly = false, finishOnDismiss = false)
              .show(requireActivity(), PinDialog.TAG)
        })
        .show()
  }

  private fun onLockTypeChangeError(throwable: Throwable) {
    Snackbreak.short(requireView(), throwable.localizedMessage)
        .show()
  }

  private fun onClearDatabase() {
    Snackbreak.short(requireView(), "Locked application database cleared")
        .show()
  }

  private fun onMasterPinClearFailure() {
    Snackbreak.short(requireView(), "Failed to clear master pin")
        .show()
  }

  private fun onMasterPinClearSuccess() {
    Snackbreak.short(requireView(), "You may now change lock type")
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
