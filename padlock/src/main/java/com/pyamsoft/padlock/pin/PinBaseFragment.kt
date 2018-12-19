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
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import javax.inject.Inject

abstract class PinBaseFragment : ToolbarFragment() {

  private var checkOnly: Boolean = false

  @field:Inject internal lateinit var viewModel: PinViewModel

  private var masterPinCheckedBeforeStart = false

  private var presenceCheckDisposable by singleDisposable()
  private var pinCheckDisposable by singleDisposable()
  private var submitDisposable by singleDisposable()

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

    return super.onCreateView(inflater, container, savedInstanceState)
  }

  @CallSuper
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    masterPinCheckedBeforeStart = false

    presenceCheckDisposable = viewModel.onMasterPinCheckEvent {
      if (it) {
        // check succeeds
        dismissParent()
      } else {
        // check fails
        onInvalidPin()
      }
    }
  }

  protected fun checkMasterPin() {
    masterPinCheckedBeforeStart = true

    // Fire initialize event once children have been created
    pinCheckDisposable = viewModel.checkMasterPin(
        onMasterPinPresent = { onMasterPinPresent() },
        onMasterPinMissing = { onMasterPinMissing() },
        onCheckError = { onCheckError() }
    )
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
      submitDisposable = viewModel.checkPin(pin)
    } else {
      submitDisposable = viewModel.submit(
          pin, reEntry, hint,
          onSubmitError = { onSubmitError(it) },
          onSubmitComplete = {
            clearDisplay()
            dismissParent()
          })
    }
  }

  private fun validateChildLifecycle() {
    if (!masterPinCheckedBeforeStart) {
      throw RuntimeException("You must call checkMasterPin() before onStart")
    }
  }

  @CallSuper
  override fun onStart() {
    super.onStart()
    validateChildLifecycle()
    clearDisplay()
  }

  @CallSuper
  override fun onDestroyView() {
    super.onDestroyView()
    presenceCheckDisposable.tryDispose()
    pinCheckDisposable.tryDispose()
    submitDisposable.tryDispose()
  }

  protected abstract fun onMasterPinMissing()

  protected abstract fun onMasterPinPresent()

  protected abstract fun onCheckError()

  protected abstract fun clearDisplay()

  protected abstract fun onSubmitError(error: Throwable)

  protected abstract fun onInvalidPin()

  abstract fun onSubmitPressed()
}
