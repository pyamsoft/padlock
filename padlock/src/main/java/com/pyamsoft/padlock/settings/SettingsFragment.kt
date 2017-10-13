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
import android.support.v7.preference.ListPreference
import android.view.View
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.settings.SettingsPresenter.Callback
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.ActionBarUtil
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class SettingsFragment : ActionBarSettingsPreferenceFragment(), Callback {

  @field:Inject internal lateinit var presenter: SettingsPresenter

  override fun provideBoundPresenters(): List<Presenter<*>> =
      listOf(presenter) + super.provideBoundPresenters()

  override val isLastOnBackStack: AboutLibrariesFragment.BackStackState
    get() = AboutLibrariesFragment.BackStackState.LAST

  override val preferenceXmlResId: Int
    get() = R.xml.preferences

  override val rootViewContainer: Int
    get() = R.id.fragment_container

  override val applicationName: String
    get() = getString(R.string.app_name)

  override fun onClearAllClicked() {
    DialogUtil.guaranteeSingleDialogFragment(activity,
        ConfirmationDialog.newInstance(ConfirmEvent.ALL), "confirm_dialog")
  }

  override fun onLicenseItemClicked() {
    ActionBarUtil.setActionBarUpEnabled(activity, true)
    super.onLicenseItemClicked()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(context.applicationContext).inject(this)

    presenter.bind(this)
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val clearDb = findPreference(getString(R.string.clear_db_key))
    val installListener = findPreference(getString(R.string.install_listener_key))
    val lockType = findPreference(getString(R.string.lock_screen_type_key)) as ListPreference


    clearDb.setOnPreferenceClickListener {
      Timber.d("Clear DB onClick")
      DialogUtil.guaranteeSingleDialogFragment(activity,
          ConfirmationDialog.newInstance(ConfirmEvent.DATABASE), "confirm_dialog")
      return@setOnPreferenceClickListener true
    }

    installListener.setOnPreferenceClickListener {
      presenter.setApplicationInstallReceiverState()
      return@setOnPreferenceClickListener true
    }

    lockType.setOnPreferenceChangeListener { _, value ->
      if (value is String) {
        presenter.checkLockType(
            onLockTypeChangeAccepted = {
              Timber.d("Change accepted, set value: %s", value)
              lockType.value = value
            }, onLockTypeChangePrevented = {
          Toasty.makeText(context, "Must clear Master Password before changing Lock Screen Type",
              Toasty.LENGTH_SHORT).show()
          DialogUtil.guaranteeSingleDialogFragment(activity,
              PinEntryDialog.newInstance(context.packageName), PinEntryDialog.TAG)
        }, onLockTypeChangeError = {
          Toasty.makeText(context, "Error: ${it.message}", Toasty.LENGTH_SHORT).show()
        })
      }

      Timber.d("Always return false here, the callback will decide if we can set value properly")
      return@setOnPreferenceChangeListener false
    }
  }

  override fun onClearDatabase() {
    Toasty.makeText(context, "Locked application database has been cleared",
        Toasty.LENGTH_SHORT).show()
  }

  override fun onClearAll() {
    Timber.d("Everything is cleared, kill self")
    presenter.publishFinish()
    val activityManager = activity.applicationContext
        .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.clearApplicationUserData()
  }

  override fun onResume() {
    super.onResume()
    setActionBarUpEnabled(false)
    setActionBarTitle(R.string.app_name)
  }

  override fun onDestroy() {
    super.onDestroy()
    PadLock.getRefWatcher(this).watch(this)
  }

  companion object {

    const val TAG = "SettingsPreferenceFragment"
  }
}
