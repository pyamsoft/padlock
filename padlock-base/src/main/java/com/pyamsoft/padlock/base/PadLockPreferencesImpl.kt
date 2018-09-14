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

package com.pyamsoft.padlock.base

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.padlock.api.preferences.ClearPreferences
import com.pyamsoft.padlock.api.preferences.InstallListenerPreferences
import com.pyamsoft.padlock.api.preferences.LockListPreferences
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.api.preferences.MasterPinPreferences
import com.pyamsoft.padlock.api.preferences.PreferenceWatcher
import com.pyamsoft.padlock.api.preferences.ServicePreferences
import com.pyamsoft.padlock.model.LockScreenType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PadLockPreferencesImpl @Inject internal constructor(
  private val context: Context
) : MasterPinPreferences,
    ClearPreferences,
    InstallListenerPreferences,
    LockListPreferences,
    LockScreenPreferences,
    ServicePreferences {

  private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

  private val ignoreTimeKey: String
  private val timeoutTimeKey: String
  private val installListener: String
  private val lockScreenType: String

  private val ignoreTimeDefault: String
  private val timeoutTimeDefault: String
  private val lockScreenTypeDefault: String
  private val installListenerDefault: Boolean

  init {
    val res = context.resources
    ignoreTimeKey = res.getString(R.string.ignore_time_key)
    ignoreTimeDefault = res.getString(R.string.ignore_time_default)
    timeoutTimeKey = res.getString(R.string.timeout_time_key)
    timeoutTimeDefault = res.getString(R.string.timeout_time_default)
    installListener = res.getString(R.string.install_listener_key)
    installListenerDefault = res.getBoolean(R.bool.install_listener_default)
    lockScreenType = res.getString(R.string.lock_screen_type_key)
    lockScreenTypeDefault = res.getString(R.string.lock_screen_type_default)
  }

  override fun getCurrentLockType(): LockScreenType =
    LockScreenType.valueOf(preferences.getString(lockScreenType, lockScreenTypeDefault).orEmpty())

  override fun isInstallListenerEnabled(): Boolean =
    preferences.getBoolean(installListener, installListenerDefault)

  override fun getHint(): String? = preferences.getString(HINT, null)

  override fun setHint(hint: String) {
    preferences.edit {
      putString(HINT, hint)
    }
  }

  override fun clearHint() {
    preferences.edit {
      remove(HINT)
    }
  }

  override fun getDefaultIgnoreTime(): Long =
    preferences.getString(ignoreTimeKey, ignoreTimeDefault).orEmpty().toLong()

  override fun getTimeoutPeriod(): Long =
    preferences.getString(timeoutTimeKey, timeoutTimeDefault).orEmpty().toLong()

  override fun isSystemVisible(): Boolean = preferences.getBoolean(IS_SYSTEM, false)

  override fun setSystemVisible(visible: Boolean) {
    preferences.edit {
      putBoolean(IS_SYSTEM, visible)
    }
  }

  override fun getMasterPassword(): String? = preferences.getString(MASTER_PASSWORD, null)

  override fun setMasterPassword(pw: String) {
    preferences.edit {
      putString(MASTER_PASSWORD, pw)
    }
  }

  override fun clearMasterPassword() {
    preferences.edit {
      remove(MASTER_PASSWORD)
    }
  }

  override fun isPaused(): Boolean {
    return preferences.getBoolean(PAUSED, false)
  }

  override fun setPaused(paused: Boolean) {
    preferences.edit {
      putBoolean(PAUSED, paused)
    }
  }

  override fun watchPausedState(func: (Boolean) -> Unit): PreferenceWatcher {
    return KeyedPreferenceWatcher(preferences, PAUSED) {
      func(isPaused())
    }
  }

  override fun watchPinPresence(func: (Boolean) -> Unit): PreferenceWatcher {
    return KeyedPreferenceWatcher(preferences, MASTER_PASSWORD) {
      func(!getMasterPassword().isNullOrEmpty())
    }
  }

  override fun watchSystemVisible(func: (Boolean) -> Unit): PreferenceWatcher {
    return KeyedPreferenceWatcher(preferences, IS_SYSTEM) {
      func(isSystemVisible())
    }
  }

  override fun clearAll() {
    preferences.edit(commit = true) {
      clear()
    }

    val defaultValuesKey = PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES
    val defaultPrefs = context.getSharedPreferences(defaultValuesKey, Context.MODE_PRIVATE)
    defaultPrefs.edit(commit = true) {
      clear()
    }
  }

  private class KeyedPreferenceWatcher internal constructor(
    private val preferences: SharedPreferences,
    private val key: String,
    func: () -> Unit
  ) : PreferenceWatcher {

    private val callback = OnSharedPreferenceChangeListener { _, preference ->
      if (key == preference) {
        func()
      }
    }

    init {
      preferences.registerOnSharedPreferenceChangeListener(callback)
    }

    override fun stopWatching() {
      Timber.d("Stop watching preference: $key")
      preferences.unregisterOnSharedPreferenceChangeListener(callback)
    }

  }

  companion object {

    private const val IS_SYSTEM = "is_system"
    private const val MASTER_PASSWORD = "master_password"
    private const val HINT = "hint"
    private const val PAUSED = "paused"
  }
}
