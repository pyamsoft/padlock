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

package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import com.pyamsoft.pydroid.ui.app.fragment.requireView
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

abstract class PinBaseFragment : ToolbarFragment() {

  private var checkOnly: Boolean = false

  @field:Inject
  internal lateinit var viewModel: PinViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkOnly = requireArguments().getBoolean(PinDialog.CHECK_ONLY, false)
  }

  @CallSuper
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPinComponent(PinModule(viewLifecycleOwner))
        .inject(this)

    // Set up observers first
    viewModel.onMasterPinMissing { onMasterPinMissing() }

    viewModel.onMasterPinPresent { onMasterPinPresent() }

    viewModel.onMasterPinSubmitError {
      // Resolve the returned view at the time of error
      Snackbreak.short(requireView(), it.localizedMessage)
    }

    viewModel.onMasterPinSubmitted {
      clearDisplay()
      dismissParent()
    }

    viewModel.onMasterPinCheckEvent {
      if (it) {
        // check succeeds
        dismissParent()
      } else {
        Snackbar.make(requireView(), "Invalid PIN", Snackbar.LENGTH_SHORT)
            .show()
      }
    }

    // Base provides no view
    return null
  }

  @CallSuper
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    // Fire initialize event once children have been created
    viewModel.checkMasterPin()
  }

  private fun dismissParent() {
    val fragmentManager: FragmentManager? = parentFragment?.fragmentManager
    val pinFragment = fragmentManager?.findFragmentByTag(PinDialog.TAG)
    if (pinFragment is PinDialog) {
      pinFragment.dismiss()
    } else {
      throw ClassCastException("Fragment is not PinDialog")
    }
  }

  protected fun submitPin(
    pin: String,
    reEntry: String,
    hint: String
  ) {
    if (checkOnly) {
      viewModel.checkPin(pin)
    } else {
      viewModel.submit(pin, reEntry, hint)
    }
  }

  abstract fun onSubmitPressed()

  protected abstract fun onMasterPinMissing()

  protected abstract fun onMasterPinPresent()

  protected abstract fun clearDisplay()

  override fun onStart() {
    super.onStart()
    clearDisplay()
  }
}
