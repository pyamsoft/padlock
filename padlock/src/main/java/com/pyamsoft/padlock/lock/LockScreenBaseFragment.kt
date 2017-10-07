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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.CheckResult
import android.support.design.widget.Snackbar
import com.pyamsoft.padlock.helper.isChecked
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_ACTIVITY_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_IS_SYSTEM
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_LOCK_CODE
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_PACKAGE_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_REAL_NAME
import com.pyamsoft.padlock.uicommon.CanaryFragment

abstract class LockScreenBaseFragment protected constructor() : CanaryFragment() {

  protected lateinit var lockedActivityName: String
  protected lateinit var lockedPackageName: String
  protected var lockedCode: String? = null
  protected lateinit var lockedRealName: String
  protected var isLockedSystem: Boolean = false

  internal fun showSnackbarWithText(text: String) {
    val activity = activity
    if (activity is LockScreenActivity) {
      Snackbar.make(activity.getRootView(), text, Snackbar.LENGTH_SHORT).show()
    }
  }

  internal val isExcluded: Boolean
    @CheckResult get() {
      val activity = activity
      return activity is LockScreenActivity && activity.menuExclude.isChecked()
    }

  internal val selectedIgnoreTime: Long
    @CheckResult get() {
      val activity = activity
      return (activity as? LockScreenActivity)?.getIgnoreTimeFromSelectedIndex() ?: 0
    }

  @CallSuper override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val bundle = arguments
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME)
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME)
    lockedRealName = bundle.getString(ENTRY_REAL_NAME)
    lockedCode = bundle.getString(ENTRY_LOCK_CODE)
    isLockedSystem = bundle.getBoolean(ENTRY_IS_SYSTEM, false)
  }

  companion object {

    @CheckResult
    @JvmStatic internal fun buildBundle(lockedPackageName: String, lockedActivityName: String,
        lockedCode: String?, lockedRealName: String, lockedSystem: Boolean): Bundle {
      val args = Bundle()
      args.putString(ENTRY_PACKAGE_NAME, lockedPackageName)
      args.putString(ENTRY_ACTIVITY_NAME, lockedActivityName)
      args.putString(ENTRY_LOCK_CODE, lockedCode)
      args.putString(ENTRY_REAL_NAME, lockedRealName)
      args.putBoolean(ENTRY_IS_SYSTEM, lockedSystem)
      return args
    }
  }
}
