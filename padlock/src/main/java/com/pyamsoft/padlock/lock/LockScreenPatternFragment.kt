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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.helper.cellPatternToString
import javax.inject.Inject

class LockScreenPatternFragment : LockScreenBaseFragment() {

  @field:Inject internal lateinit var lockScreen: LockScreenPatternView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    inject()
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .build()
        .inject(this)

    lockScreen.create()
    return lockScreen.root()
  }

  override fun showSnackbarWithText(text: String) {
    lockScreen.showSnackbar(text)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    lockScreen.onPatternComplete {
      submitPin(cellPatternToString(it))
      clearDisplay()
    }
  }

  override fun onStart() {
    super.onStart()
    clearDisplay()
  }

  override fun clearDisplay() {
    lockScreen.clearDisplay()
  }

  override fun onDisplayHint(hint: String) {
    // No hints for pattern fragment
  }

  companion object {

    const val TAG = "LockScreenPatternFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(
      lockedCode: String?,
      lockedSystem: Boolean
    ): LockScreenPatternFragment {
      val fragment = LockScreenPatternFragment()
      fragment.arguments = LockScreenBaseFragment.buildBundle(lockedCode, lockedSystem)
      return fragment
    }
  }
}
