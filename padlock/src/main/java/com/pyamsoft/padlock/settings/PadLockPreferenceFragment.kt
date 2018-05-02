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
import android.support.design.widget.Snackbar
import android.support.v7.preference.ListPreference
import android.view.View
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.pydroid.ui.app.fragment.SettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.Snackbreak.ErrorDetail
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PadLockPreferenceFragment : SettingsPreferenceFragment(), SettingsPresenter.View {

  @field:Inject
  internal lateinit var presenter: SettingsPresenter
  private lateinit var lockType: ListPreference

  override val preferenceXmlResId: Int = R.xml.preferences

  override val rootViewContainer: Int = R.id.fragment_container

  override val applicationName: String
    get() = getString(R.string.app_name)

  override fun onClearAllClicked() {
    ConfirmationDialog.newInstance(ConfirmEvent.ALL)
        .show(requireActivity(), "confirm_dialog")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .inject(this)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    val clearDb = findPreference(getString(R.string.clear_db_key))
    val installListener = findPreference(getString(R.string.install_listener_key))
    lockType = findPreference(getString(R.string.lock_screen_type_key)) as ListPreference

    clearDb.setOnPreferenceClickListener {
      Timber.d("Clear DB onClick")
      ConfirmationDialog.newInstance(ConfirmEvent.DATABASE)
          .show(requireActivity(), "confirm_dialog")
      return@setOnPreferenceClickListener true
    }

    installListener.setOnPreferenceClickListener {
      presenter.setApplicationInstallReceiverState()
      return@setOnPreferenceClickListener true
    }

    lockType.setOnPreferenceChangeListener { _, value ->
      if (value is String) {
        presenter.checkLockType(value)
      }

      Timber.d(
          "Always return false here, the callback will decide if we can set value properly"
      )
      return@setOnPreferenceChangeListener false
    }

    presenter.bind(viewLifecycle, this)
  }

  override fun onLockTypeChangeAccepted(value: String) {
    Timber.d("Change accepted, set value: %s", value)
    lockType.value = value
  }

  override fun onLockTypeChangePrevented() {
    Snackbar.make(
        requireView(),
        "You must clear the current PIN before changing type",
        Snackbar.LENGTH_LONG
    )
        .apply {
          setAction("Okay", DebouncedOnClickListener.create {
            PinEntryDialog.newInstance(requireContext().packageName)
                .show(requireActivity(), PinEntryDialog.TAG)
          })
        }
        .show()
  }

  override fun onLockTypeChangeError(throwable: Throwable) {
    Snackbreak.short(requireActivity(), requireView(), ErrorDetail("", throwable.localizedMessage))
  }

  override fun onClearDatabase() {
    Snackbar.make(requireView(), "Locked application database cleared", Snackbar.LENGTH_SHORT)
        .show()
  }

  override fun onMasterPinClearFailure() {
    Snackbar.make(requireView(), "Failed to clear master pin", Snackbar.LENGTH_SHORT)
        .show()
  }

  override fun onMasterPinClearSuccess() {
    Snackbar.make(requireView(), "You may now change lock type", Snackbar.LENGTH_SHORT)
        .show()
  }

  override fun onClearAll() {
    Timber.d("Everything is cleared, kill self")
    val activityManager = activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.clearApplicationUserData()
  }

  override fun onResume() {
    super.onResume()
    toolbarActivity.withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    PadLock.getRefWatcher(this)
        .watch(this)
  }

  companion object {

    const val TAG = "PadLockPreferenceFragment"
  }
}
