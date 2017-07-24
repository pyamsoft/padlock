/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.view.View
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment
import com.pyamsoft.pydroid.ui.helper.ProgressOverlay
import com.pyamsoft.pydroid.ui.helper.ProgressOverlayHelper
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.ActionBarUtil
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class SettingsFragment : ActionBarSettingsPreferenceFragment() {

  @field:Inject internal lateinit var presenter: SettingsPreferencePresenter
  protected @JvmField var overlay = ProgressOverlay.empty()
  private lateinit var clearDb: Preference
  private lateinit var installListener: Preference
  private lateinit var lockType: ListPreference

  override val isLastOnBackStack: AboutLibrariesFragment.BackStackState
    get() = AboutLibrariesFragment.BackStackState.LAST

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Injector.with(context) {
      it.inject(this)
    }
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    clearDb = findPreference(getString(R.string.clear_db_key))
    installListener = findPreference(getString(R.string.install_listener_key))
    lockType = findPreference(getString(R.string.lock_screen_type_key)) as ListPreference
  }

  override val preferenceXmlResId: Int
    get() = R.xml.preferences

  override val rootViewContainer: Int
    get() = R.id.fragment_container

  override val applicationName: String
    get() = getString(R.string.app_name)

  override fun onClearAllClicked() {
    DialogUtil.guaranteeSingleDialogFragment(activity,
        ConfirmationDialog.newInstance(ConfirmEvent.Type.ALL), "confirm_dialog")
  }

  override fun onLicenseItemClicked() {
    ActionBarUtil.setActionBarUpEnabled(activity, true)
    super.onLicenseItemClicked()
  }

  override fun onStart() {
    super.onStart()

    presenter.clickEvent(clearDb, {
      Timber.d("Clear DB onClick")
      DialogUtil.guaranteeSingleDialogFragment(activity,
          ConfirmationDialog.newInstance(ConfirmEvent.Type.DATABASE), "confirm_dialog")
    })

    presenter.clickEvent(installListener, {
      presenter.setApplicationInstallReceiverState()
    })

    presenter.preferenceChangedEvent<String>(lockType, { _, value ->
      presenter.checkLockType(onBegin = {
        overlay = ProgressOverlayHelper.dispose(overlay)
        overlay = ProgressOverlay.builder().build(activity)
      }, onLockTypeChangeAccepted = {
        Timber.d("Change accepted, set value: %s", value)
        lockType.value = value
      }, onLockTypeChangePrevented = {
        Toasty.makeText(context, "Must clear Master Password before changing Lock Screen Type",
            Toasty.LENGTH_SHORT).show()
        DialogUtil.guaranteeSingleDialogFragment(activity,
            PinEntryDialog.newInstance(context.packageName), PinEntryDialog.TAG)
      }, onLockTypeChangeError = {
        Toasty.makeText(context, "Error: ${it.message}", Toasty.LENGTH_SHORT).show()
      }, onEnd = {
        overlay = ProgressOverlayHelper.dispose(overlay)
      })
    }, {
      Timber.d("Always return false here, the callback will decide if we can set value properly")
      return@preferenceChangedEvent false
    })

    presenter.registerOnBus(onClearAll = {
      Timber.d("Everything is cleared, kill self")
      try {
        PadLockService.finish()
      } catch (e: NullPointerException) {
        Timber.e(e, "Expected NPE when Service is NULL")
      }

      val activityManager = activity.applicationContext
          .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      activityManager.clearApplicationUserData()
    }, onClearDatabase = {
      Toasty.makeText(context, "Locked application database has been cleared",
          Toasty.LENGTH_SHORT).show()
    })
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
  }

  override fun onResume() {
    super.onResume()
    setActionBarUpEnabled(false)
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.destroy()
  }

  companion object {

    const val TAG = "SettingsPreferenceFragment"
  }
}
