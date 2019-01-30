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

package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.pydroid.ui.app.fragment.requireView
import com.pyamsoft.pydroid.ui.util.Snackbreak
import timber.log.Timber
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class PinTextFragment : PinBaseFragment() {

  @field:Inject internal lateinit var pinView: PinTextView

  private val imm by lazy(NONE) {
    requireNotNull(requireContext().getSystemService<InputMethodManager>())
  }

  private val submitCallback: (String, String, String) -> Unit = { attempt, reEntry, hint ->
    submitPin(attempt, reEntry, hint)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireActivity().applicationContext)
        .plusPinComponent()
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .build()
        .inject(this)

    super.onCreateView(inflater, container, savedInstanceState)
    pinView.create()
    return pinView.root()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // Force the keyboard
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)

    clearDisplay()
    setupGoArrow()

    checkMasterPin()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    activity?.let {
      imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
    }
  }

  /**
   * Clear the display of all text entry fields
   */
  override fun clearDisplay() {
    pinView.clearDisplay()
  }

  override fun onMasterPinMissing() {
    Timber.d("No active master, show extra views")
    pinView.showReEntry(submitCallback)
  }

  override fun onMasterPinPresent() {
    Timber.d("Active master, hide extra views")
    pinView.hideReEntry(submitCallback)
  }

  override fun onSubmitPressed() {
    pinView.onSubmitPressed(submitCallback)
  }

  private fun setupGoArrow() {
    // Force keyboard focus
    pinView.focus()
  }

  override fun onInvalidPin() {
    Snackbreak.bindTo(viewLifecycleOwner)
        .short(requireView(), "Error incorrect PIN")
        .show()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    pinView.saveState(outState)
    super.onSaveInstanceState(outState)
  }

  companion object {

    internal const val TAG = "PinTextFragment"

    @CheckResult
    @JvmStatic
    fun newInstance(checkOnly: Boolean): PinTextFragment {
      return PinTextFragment().apply {
        arguments = Bundle().apply {
          putBoolean(PinDialog.CHECK_ONLY, checkOnly)
        }
      }
    }
  }
}
