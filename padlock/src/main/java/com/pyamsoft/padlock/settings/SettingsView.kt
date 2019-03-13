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
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.arch.PrefUiView
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import timber.log.Timber
import javax.inject.Inject

internal class SettingsView @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val view: View,
  preferenceScreen: PreferenceScreen,
  callback: SettingsView.Callback
) : PrefUiView<SettingsView.Callback>(preferenceScreen, callback) {

  private val lockType by lazyPref<ListPreference>(R.string.lock_screen_type_key)
  private val installListener by lazyPref<Preference>(R.string.install_listener_key)
  private val clearDb by lazyPref<Preference>(R.string.clear_db_key)

  override fun onInflated(
    preferenceScreen: PreferenceScreen,
    savedInstanceState: Bundle?
  ) {
    lockType.setOnPreferenceChangeListener { _, value ->
      if (value is String) {
        callback.onSwitchLockTypeChanged(value)
        return@setOnPreferenceChangeListener false
      } else {
        return@setOnPreferenceChangeListener false
      }
    }

    installListener.setOnPreferenceClickListener {
      callback.onInstallListenerClicked()
      return@setOnPreferenceClickListener true
    }

    clearDb.setOnPreferenceClickListener {
      callback.onClearDatabaseClicked()
      return@setOnPreferenceClickListener true
    }
  }

  override fun onTeardown() {
    lockType.onPreferenceChangeListener = null
    installListener.onPreferenceClickListener = null
    clearDb.onPreferenceClickListener = null
  }

  fun changeLockType(newValue: String) {
    Timber.d("Change lock type: $newValue")
    lockType.value = newValue
  }

  fun showMessage(message: String) {
    Snackbreak.bindTo(owner)
        .short(view, message)
        .show()
  }

  inline fun promptChangeLockType(crossinline func: () -> Unit) {
    Snackbreak.bindTo(owner)
        .long(view, "You must clear the current code before changing type")
        .setAction("Clear", DebouncedOnClickListener.create { func() })
        .show()
  }

  interface Callback {

    fun onSwitchLockTypeChanged(newType: String)

    fun onInstallListenerClicked()

    fun onClearDatabaseClicked()
  }
}

