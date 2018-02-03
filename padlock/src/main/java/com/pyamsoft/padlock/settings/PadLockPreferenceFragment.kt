/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.preference.ListPreference
import android.view.View
import android.widget.Toast
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.main.MainFragment
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.pydroid.ui.app.fragment.SettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.DialogUtil
import com.pyamsoft.pydroid.ui.util.setUpEnabled
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

  override val aboutReplaceFragment: Fragment?
    get() = activity?.supportFragmentManager?.findFragmentByTag(MainFragment.TAG)

  override fun onClearAllClicked() {
    DialogUtil.guaranteeSingleDialogFragment(
        activity,
        ConfirmationDialog.newInstance(ConfirmEvent.ALL), "confirm_dialog"
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(context!!.applicationContext)
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
      DialogUtil.guaranteeSingleDialogFragment(
          activity,
          ConfirmationDialog.newInstance(ConfirmEvent.DATABASE), "confirm_dialog"
      )
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
    context!!.let {
      Toasty.makeText(
          it, "Must clear Master Password before changing Lock Screen Type",
          Toasty.LENGTH_SHORT
      )
          .show()
      DialogUtil.guaranteeSingleDialogFragment(
          activity,
          PinEntryDialog.newInstance(it.packageName), PinEntryDialog.TAG
      )
    }
  }

  override fun onLockTypeChangeError(throwable: Throwable) {
    Toasty.makeText(context!!, "Error: ${throwable.message}", Toasty.LENGTH_SHORT)
        .show()
  }

  override fun onClearDatabase() {
    Toasty.makeText(
        context!!, "Locked application database has been cleared",
        Toasty.LENGTH_SHORT
    )
        .show()
  }

  override fun onMasterPinClearFailure() {
    Toasty.makeText(context!!, "Error: Invalid PIN", Toast.LENGTH_SHORT)
        .show()
  }

  override fun onMasterPinClearSuccess() {
    val v = view
    if (v != null) {
      Snackbar.make(v, "You may now change lock type", Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  override fun onClearAll() {
    Timber.d("Everything is cleared, kill self")
    val activityManager = activity!!.applicationContext
        .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
