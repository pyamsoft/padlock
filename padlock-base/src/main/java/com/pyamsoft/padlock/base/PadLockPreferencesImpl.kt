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

package com.pyamsoft.padlock.base

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.preference.PreferenceManager
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.model.LockScreenType
import javax.inject.Inject

internal class PadLockPreferencesImpl @Inject internal constructor(
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
    ignoreTimeKey = res.getString(R.string.ignore_time_key)
    ignoreTimeDefault = res.getString(R.string.ignore_time_default)
    timeoutTimeKey = res.getString(R.string.timeout_time_key)
    timeoutTimeDefault = res.getString(R.string.timeout_time_default)
    lockPackageChangeKey = res.getString(R.string.lock_package_change_key)
    lockPackageChangeDefault = res.getBoolean(R.bool.lock_package_change_default)
    installListener = res.getString(R.string.install_listener_key)
    installListenerDefault = res.getBoolean(R.bool.install_listener_default)
    ignoreKeyguard = res.getString(R.string.ignore_keyguard_key)
    ignoreKeyguardDefault = res.getBoolean(R.bool.ignore_keyguard_default)
    lockScreenType = res.getString(R.string.lock_screen_type_key)
    lockScreenTypeDefault = res.getString(R.string.lock_screen_type_default)
  }

  override fun getCurrentLockType(): LockScreenType {
    return LockScreenType.valueOf(preferences.getString(lockScreenType, lockScreenTypeDefault))
  }

  override fun isIgnoreInKeyguard(): Boolean {
    return preferences.getBoolean(ignoreKeyguard, ignoreKeyguardDefault)
  }

  override fun isInstallListenerEnabled(): Boolean {
    return preferences.getBoolean(installListener, installListenerDefault)
  }

  override fun getHint(): String? {
    return preferences.getString(HINT, null)
  }

  override fun setHint(hint: String) {
    preferences.edit().putString(HINT, hint).apply()
  }

  override fun clearHint() {
    preferences.edit().remove(HINT).apply()
  }

  override fun isInfoDialogOnBoard(): Boolean {
    return preferences.getBoolean(LOCK_DIALOG_ONBOARD, false)
  }

  override fun getDefaultIgnoreTime(): Long {
    return java.lang.Long.parseLong(preferences.getString(ignoreTimeKey, ignoreTimeDefault))
  }

  override fun getTimeoutPeriod(): Long {
    return java.lang.Long.parseLong(preferences.getString(timeoutTimeKey, timeoutTimeDefault))
  }

  override fun isLockOnPackageChange(): Boolean {
    return preferences.getBoolean(lockPackageChangeKey, lockPackageChangeDefault)
  }

  override fun isSystemVisible(): Boolean {
    return preferences.getBoolean(IS_SYSTEM, false)
  }

  override fun setSystemVisible(visible: Boolean) {
    preferences.edit().putBoolean(IS_SYSTEM, visible).apply()
  }

  override fun getMasterPassword(): String? {
    return preferences.getString(MASTER_PASSWORD, null)
  }

  override fun setMasterPassword(pw: String) {
    preferences.edit().putString(MASTER_PASSWORD, pw).apply()
  }

  override fun clearMasterPassword() {
    preferences.edit().remove(MASTER_PASSWORD).apply()
  }

  override fun hasAgreed(): Boolean {
    return preferences.getBoolean(AGREED, false)
  }

  override fun setAgreed() {
    preferences.edit().putBoolean(AGREED, true).apply()
  }

  override fun isListOnBoard(): Boolean {
    return preferences.getBoolean(LOCK_LIST_ONBOARD, false)
  }

  override fun setListOnBoard() {
    preferences.edit().putBoolean(LOCK_LIST_ONBOARD, true).apply()
  }

  override fun setInfoDialogOnBoard() {
    preferences.edit().putBoolean(LOCK_DIALOG_ONBOARD, true).apply()
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
