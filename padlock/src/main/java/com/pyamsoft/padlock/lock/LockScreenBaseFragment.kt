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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.CheckResult
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_ACTIVITY_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_IS_SYSTEM
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_LOCK_CODE
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_PACKAGE_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_REAL_NAME

abstract class LockScreenBaseFragment protected constructor() : Fragment() {

  protected lateinit var lockedActivityName: String
  protected lateinit var lockedPackageName: String
  protected lateinit var lockedCode: String
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
      return activity is LockScreenActivity && activity.menuExclude.isChecked
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
