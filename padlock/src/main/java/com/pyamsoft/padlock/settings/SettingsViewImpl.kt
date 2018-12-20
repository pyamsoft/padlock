package com.pyamsoft.padlock.settings

import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.pyamsoft.padlock.R
import timber.log.Timber
import javax.inject.Inject

internal class SettingsViewImpl @Inject internal constructor(
  private val preferenceScreen: PreferenceScreen
) : SettingsView {

  private val context = preferenceScreen.context

  private lateinit var lockType: ListPreference
  private lateinit var installListener: Preference
  private lateinit var clearDb: Preference

  @CheckResult
  private fun findPreference(@StringRes id: Int): Preference {
    return preferenceScreen.findPreference(context.getString(id))
  }

  override fun create() {
    lockType = findPreference(R.string.lock_screen_type_key) as ListPreference
    installListener = findPreference(R.string.install_listener_key)
    clearDb = findPreference(R.string.clear_db_key)
  }

  override fun onLockTypeChangeAttempt(onChange: (newValue: String) -> Unit) {
    lockType.setOnPreferenceChangeListener { _, value ->
      if (value is String) {
        onChange(value)
        return@setOnPreferenceChangeListener false
      } else {
        return@setOnPreferenceChangeListener false
      }
    }
  }

  override fun onInstallListenerClicked(onClick: () -> Unit) {
    installListener.setOnPreferenceClickListener {
      onClick()
      return@setOnPreferenceClickListener true
    }
  }

  override fun onClearDatabaseClicked(onClick: () -> Unit) {
    clearDb.setOnPreferenceClickListener {
      onClick()
      return@setOnPreferenceClickListener true
    }
  }

  override fun changeLockType(newValue: String) {
    Timber.d("Change lock type: $newValue")
    lockType.value = newValue
  }
}
