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

package com.pyamsoft.padlock.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import com.pyamsoft.padlock.api.preferences.ClearPreferences
import com.pyamsoft.padlock.api.preferences.InstallListenerPreferences
import com.pyamsoft.padlock.api.preferences.LockListPreferences
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.api.preferences.MasterPinPreferences
import com.pyamsoft.padlock.api.preferences.PreferenceWatcher
import com.pyamsoft.padlock.api.preferences.ServicePreferences
import com.pyamsoft.padlock.model.LockScreenType
import com.pyamsoft.padlock.model.service.ServicePauseState
import com.pyamsoft.padlock.model.service.ServicePauseState.STARTED
import com.pyamsoft.padlock.model.service.ServicePauseState.valueOf
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
  private val globalPreferenceWatcher by lazy { GlobalPreferenceWatcher(preferences) }

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

  private inline fun edit(onEdit: SharedPreferences.Editor.() -> Unit) {
    preferences.edit()
        .apply(onEdit)
        .apply()
  }

  override fun getCurrentLockType(): LockScreenType =
    LockScreenType.valueOf(preferences.getString(lockScreenType, lockScreenTypeDefault).orEmpty())

  override fun isInstallListenerEnabled(): Boolean =
    preferences.getBoolean(installListener, installListenerDefault)

  override fun getHint(): String? = preferences.getString(HINT, null)

  override fun setHint(hint: String) {
    edit {
      putString(HINT, hint)
    }
  }

  override fun clearHint() {
    edit {
      remove(HINT)
    }
  }

  override fun getDefaultIgnoreTime(): Long =
    preferences.getString(ignoreTimeKey, ignoreTimeDefault).orEmpty().toLong()

  override fun getTimeoutPeriod(): Long =
    preferences.getString(timeoutTimeKey, timeoutTimeDefault).orEmpty().toLong()

  override fun isSystemVisible(): Boolean = preferences.getBoolean(IS_SYSTEM, false)

  override fun setSystemVisible(visible: Boolean) {
    edit {
      putBoolean(IS_SYSTEM, visible)
    }
  }

  override fun getMasterPassword(): String? = preferences.getString(MASTER_PASSWORD, null)

  override fun setMasterPassword(pw: String) {
    edit {
      putString(MASTER_PASSWORD, pw)
    }
  }

  override fun clearMasterPassword() {
    edit {
      remove(MASTER_PASSWORD)
    }
  }

  override fun getPaused(): ServicePauseState {
    return valueOf(preferences.getString(PAUSED, STARTED.name).orEmpty())
  }

  override fun setPaused(paused: ServicePauseState) {
    edit {
      putString(PAUSED, paused.name)
    }
  }

  override fun watchPausedState(func: (ServicePauseState) -> Unit): PreferenceWatcher {
    return KeyedPreferenceWatcher(globalPreferenceWatcher, PAUSED) {
      func(getPaused())
    }
  }

  override fun watchPinPresence(func: (Boolean) -> Unit): PreferenceWatcher {
    return KeyedPreferenceWatcher(globalPreferenceWatcher, MASTER_PASSWORD) {
      func(!getMasterPassword().isNullOrEmpty())
    }
  }

  override fun watchSystemVisible(func: (Boolean) -> Unit): PreferenceWatcher {
    return KeyedPreferenceWatcher(globalPreferenceWatcher, IS_SYSTEM) {
      func(isSystemVisible())
    }
  }

  @SuppressLint("ApplySharedPref")
  private fun clear(preferences: SharedPreferences) {
    preferences.edit()
        .clear()
        .commit()
  }

  override fun clearAll() {
    val defaultValuesKey = PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES
    val defaultPrefs = context.getSharedPreferences(defaultValuesKey, Context.MODE_PRIVATE)

    clear(preferences)
    clear(defaultPrefs)
  }

  private class GlobalPreferenceWatcher internal constructor(
    private val preferences: SharedPreferences
  ) {

    private val watcherMap = ConcurrentHashMap<UUID, Pair<String, () -> Unit>>()

    private val callback = OnSharedPreferenceChangeListener { _, preference ->
      val values = watcherMap.values
      for (entry in values) {
        val key = entry.first
        val func = entry.second
        if (key == preference) {
          func()
        }
      }
    }

    fun addWatcher(
      uuid: UUID,
      key: String,
      func: () -> Unit
    ) {
      if (watcherMap.isEmpty()) {
        preferences.registerOnSharedPreferenceChangeListener(callback)
      }

      Timber.d("Adding PreferenceWatcher for key $key")
      watcherMap[uuid] = key to func
    }

    fun removeWatcher(uuid: UUID) {
      watcherMap.remove(uuid)
          ?.also { (key, _) ->
            Timber.d("Removing PreferenceWatcher for key $key")
          }

      if (watcherMap.isEmpty()) {
        Timber.d("Stop watching preferences")
        preferences.unregisterOnSharedPreferenceChangeListener(callback)
      }
    }
  }

  private class KeyedPreferenceWatcher(
    private val globalPreferenceWatcher: GlobalPreferenceWatcher,
    key: String,
    func: () -> Unit
  ) : PreferenceWatcher {

    private val uuid = UUID.randomUUID()

    init {
      globalPreferenceWatcher.addWatcher(uuid, key, func)
    }

    override fun stopWatching() {
      globalPreferenceWatcher.removeWatcher(uuid)
    }

  }

  companion object {

    private const val IS_SYSTEM = "is_system"
    private const val MASTER_PASSWORD = "master_password"
    private const val HINT = "hint"
    private const val PAUSED = "paused_state"
  }
}
