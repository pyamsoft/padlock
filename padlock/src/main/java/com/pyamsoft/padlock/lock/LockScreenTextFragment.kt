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
import android.view.inputmethod.InputMethodManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class LockScreenTextFragment : LockScreenBaseFragment() {

  @field:Inject internal lateinit var lockScreen: LockScreenTextView

  private val imm by lazy(NONE) {
    requireNotNull(requireActivity().application.getSystemService<InputMethodManager>())
  }

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

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupInputManager()

    lockScreen.onGoClicked { currentAttempt: String ->
      submitPin(currentAttempt)
      activity?.also {
        imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
      }
    }

    lockScreen.onEnterKeyPressed { submitPin(it) }

    lockScreen.onFocusClaimed {
      imm.toggleSoftInput(
          InputMethodManager.SHOW_FORCED,
          InputMethodManager.HIDE_IMPLICIT_ONLY
      )
    }

    clearDisplay()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    activity?.let {
      imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    lockScreen.saveState(outState)
    super.onSaveInstanceState(outState)
  }

  private fun setupInputManager() {
    // Force the keyboard
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
  }

  override fun onDisplayHint(hint: String) {
    lockScreen.displayHint(hint)
  }

  override fun clearDisplay() {
    lockScreen.clearDisplay()
  }

  override fun showSnackbarWithText(text: String) {
    lockScreen.showSnackbar(text)
  }

  companion object {

    internal const val TAG = "LockScreenTextFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(
      lockedCode: String?,
      lockedSystem: Boolean
    ): LockScreenTextFragment {
      val fragment = LockScreenTextFragment()
      fragment.arguments = LockScreenBaseFragment.buildBundle(lockedCode, lockedSystem)
      return fragment
    }
  }
}
