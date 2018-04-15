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
import android.support.v7.preference.PreferenceManager
import androidx.core.content.edit
import com.pyamsoft.padlock.api.ClearPreferences
import com.pyamsoft.padlock.api.InstallListenerPreferences
import com.pyamsoft.padlock.api.LockListPreferences
import com.pyamsoft.padlock.api.LockScreenPreferences
import com.pyamsoft.padlock.api.MasterPinPreferences
import com.pyamsoft.padlock.api.OnboardingPreferences
import com.pyamsoft.padlock.model.LockScreenType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PadLockPreferencesImpl @Inject internal constructor(
  context: Context
) : MasterPinPreferences,
    ClearPreferences,
    InstallListenerPreferences,
    LockListPreferences,
    LockScreenPreferences,
    OnboardingPreferences {

  private val ignoreTimeKey: String
  private val ignoreTimeDefault: String
  private val timeoutTimeKey: String
  private val timeoutTimeDefault: String
  private val installListener: String
  private val ignoreKeyguard: String
  private val lockScreenType: String
  private val lockScreenTypeDefault: String
  private val installListenerDefault: Boolean
  private val ignoreKeyguardDefault: Boolean
  private val preferences: SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)

  init {
    val res = context.resources
    ignoreTimeKey = res.getString(R.string.ignore_time_key)
    ignoreTimeDefault = res.getString(R.string.ignore_time_default)
    timeoutTimeKey = res.getString(R.string.timeout_time_key)
    timeoutTimeDefault = res.getString(R.string.timeout_time_default)
    installListener = res.getString(R.string.install_listener_key)
    installListenerDefault = res.getBoolean(R.bool.install_listener_default)
    ignoreKeyguard = res.getString(R.string.ignore_keyguard_key)
    ignoreKeyguardDefault = res.getBoolean(R.bool.ignore_keyguard_default)
    lockScreenType = res.getString(R.string.lock_screen_type_key)
    lockScreenTypeDefault = res.getString(R.string.lock_screen_type_default)
  }

  override fun getCurrentLockType(): LockScreenType =
    LockScreenType.valueOf(preferences.getString(lockScreenType, lockScreenTypeDefault))

  override fun isIgnoreInKeyguard(): Boolean =
    preferences.getBoolean(ignoreKeyguard, ignoreKeyguardDefault)

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

  override fun isInfoDialogOnBoard(): Boolean = preferences.getBoolean(LOCK_DIALOG_ONBOARD, false)

  override fun getDefaultIgnoreTime(): Long = preferences.getString(
      ignoreTimeKey, ignoreTimeDefault
  ).toLong()

  override fun getTimeoutPeriod(): Long = preferences.getString(
      timeoutTimeKey, timeoutTimeDefault
  ).toLong()

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

  override fun hasAgreed(): Boolean = preferences.getBoolean(AGREED, false)

  override fun setAgreed() {
    preferences.edit {
      putBoolean(AGREED, true)
    }
  }

  override fun isListOnBoard(): Boolean = preferences.getBoolean(LOCK_LIST_ONBOARD, false)

  override fun setListOnBoard() {
    preferences.edit {
      putBoolean(LOCK_LIST_ONBOARD, true)
    }
  }

  override fun setInfoDialogOnBoard() {
    preferences.edit {
      putBoolean(LOCK_DIALOG_ONBOARD, true)
    }
  }

  override fun clearAll() {
    preferences.edit(commit = true) {
      clear()
    }
  }

  companion object {

    private const val IS_SYSTEM = "is_system"
    private const val MASTER_PASSWORD = "master_password"
    private const val HINT = "hint"
    private const val AGREED = "agreed"
    private const val LOCK_LIST_ONBOARD = "list_onboard"
    private const val LOCK_DIALOG_ONBOARD = "dialog_onboard"
  }
}
