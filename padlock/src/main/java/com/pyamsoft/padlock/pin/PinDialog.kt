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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CheckResult
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.app.noTitle
import com.pyamsoft.pydroid.ui.app.requireArguments
import javax.inject.Inject

class PinDialog : DialogFragment() {

  @field:Inject internal lateinit var pinView: CreatePinView

  private var checkOnly: Boolean = false
  private var finishOnDismiss: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true

    checkOnly = requireArguments().getBoolean(CHECK_ONLY, false)
    finishOnDismiss = requireArguments().getBoolean(FINISH_ON_DISMISS, false)
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    dialog.window?.apply {
      setLayout(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT
      )
      setGravity(Gravity.CENTER)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = inflater.inflate(R.layout.layout_frame, container, false)

    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPinComponent()
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .build()
        .inject(this)

    return root
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return super.onCreateDialog(savedInstanceState)
        .noTitle()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    pinView.inflate(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    pinView.saveState(outState)
  }

  override fun onDismiss(dialog: DialogInterface?) {
    super.onDismiss(dialog)
    if (finishOnDismiss) {
      activity?.also { it.finish() }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    pinView.teardown()
  }

  companion object {

    const val TAG = "PinDialog"
    internal const val CHECK_ONLY = "check_only"
    private const val FINISH_ON_DISMISS = "finish_dismiss"

    @JvmStatic
    @CheckResult
    fun newInstance(
      checkOnly: Boolean,
      finishOnDismiss: Boolean
    ): PinDialog {
      return PinDialog().apply {
        arguments = Bundle().apply {
          putBoolean(CHECK_ONLY, checkOnly)
          putBoolean(FINISH_ON_DISMISS, finishOnDismiss)
        }
      }
    }
  }
}

