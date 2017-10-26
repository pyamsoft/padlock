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

package com.pyamsoft.padlock.base.preference

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.preference.PreferenceManager
import com.pyamsoft.padlock.base.R.bool
import com.pyamsoft.padlock.base.R.string
import com.pyamsoft.padlock.model.LockScreenType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class PadLockPreferencesImpl @Inject internal constructor(
    context: Context) : MasterPinPreferences, ClearPreferences, InstallListenerPreferences,
    LockListPreferences, LockScreenPreferences, OnboardingPreferences {
  private val preferences: SharedPreferences
  private val ignoreTimeKey: String
  private val ignoreTimeDefault: String
  private val timeoutTimeKey: String
  private val timeoutTimeDefault: String
  private val lockPackageChangeKey: String
  private val installListener: String
  private val ignoreKeyguard: String
  private val lockScreenType: String
  private val lockScreenTypeDefault: String
  private val lockPackageChangeDefault: Boolean
  private val installListenerDefault: Boolean
  private val ignoreKeyguardDefault: Boolean

  init {
    val appContext = context.applicationContext
    val res = appContext.resources

    this.preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
    ignoreTimeKey = res.getString(string.ignore_time_key)
    ignoreTimeDefault = res.getString(string.ignore_time_default)
    timeoutTimeKey = res.getString(string.timeout_time_key)
    timeoutTimeDefault = res.getString(string.timeout_time_default)
    lockPackageChangeKey = res.getString(string.lock_package_change_key)
    lockPackageChangeDefault = res.getBoolean(
        bool.lock_package_change_default)
    installListener = res.getString(string.install_listener_key)
    installListenerDefault = res.getBoolean(
        bool.install_listener_default)
    ignoreKeyguard = res.getString(string.ignore_keyguard_key)
    ignoreKeyguardDefault = res.getBoolean(bool.ignore_keyguard_default)
    lockScreenType = res.getString(string.lock_screen_type_key)
    lockScreenTypeDefault = res.getString(
        string.lock_screen_type_default)
  }

  override fun getCurrentLockType(): LockScreenType =
      LockScreenType.valueOf(preferences.getString(lockScreenType, lockScreenTypeDefault))

  override fun isIgnoreInKeyguard(): Boolean =
      preferences.getBoolean(ignoreKeyguard, ignoreKeyguardDefault)

  override fun isInstallListenerEnabled(): Boolean =
      preferences.getBoolean(installListener, installListenerDefault)

  override fun getHint(): String? = preferences.getString(
      HINT, null)

  override fun setHint(hint: String) {
    preferences.edit().putString(
        HINT, hint).apply()
  }

  override fun clearHint() {
    preferences.edit().remove(
        HINT).apply()
  }

  override fun isInfoDialogOnBoard(): Boolean = preferences.getBoolean(
      LOCK_DIALOG_ONBOARD, false)

  override fun getDefaultIgnoreTime(): Long =
      preferences.getString(ignoreTimeKey, ignoreTimeDefault).toLong()

  override fun getTimeoutPeriod(): Long =
      preferences.getString(timeoutTimeKey, timeoutTimeDefault).toLong()

  override fun isLockOnPackageChange(): Boolean =
      preferences.getBoolean(lockPackageChangeKey, lockPackageChangeDefault)

  override fun isSystemVisible(): Boolean = preferences.getBoolean(
      IS_SYSTEM, false)

  override fun setSystemVisible(visible: Boolean) {
    preferences.edit().putBoolean(
        IS_SYSTEM, visible).apply()
  }

  override fun getMasterPassword(): String? = preferences.getString(
      MASTER_PASSWORD, null)

  override fun setMasterPassword(pw: String) {
    preferences.edit().putString(
        MASTER_PASSWORD, pw).apply()
  }

  override fun clearMasterPassword() {
    preferences.edit().remove(
        MASTER_PASSWORD).apply()
  }

  override fun hasAgreed(): Boolean = preferences.getBoolean(
      AGREED, false)

  override fun setAgreed() {
    preferences.edit().putBoolean(
        AGREED, true).apply()
  }

  override fun isListOnBoard(): Boolean = preferences.getBoolean(
      LOCK_LIST_ONBOARD, false)

  override fun setListOnBoard() {
    preferences.edit().putBoolean(
        LOCK_LIST_ONBOARD, true).apply()
  }

  override fun setInfoDialogOnBoard() {
    preferences.edit().putBoolean(
        LOCK_DIALOG_ONBOARD, true).apply()
  }

  override fun clearAll() {
    preferences.edit().clear().apply()
  }

  companion object {

    const private val IS_SYSTEM = "is_system"
    const private val MASTER_PASSWORD = "master_password"
    const private val HINT = "hint"
    const private val AGREED = "agreed"
    const private val LOCK_LIST_ONBOARD = "list_onboard"
    const private val LOCK_DIALOG_ONBOARD = "dialog_onboard"
  }
}