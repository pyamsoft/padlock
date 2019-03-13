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

package com.pyamsoft.padlock.pin.confirm

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.R.layout
import com.pyamsoft.padlock.pin.toolbar.PinToolbarUiComponent
import com.pyamsoft.padlock.pin.toolbar.PinToolbarUiComponent.Callback
import com.pyamsoft.pydroid.ui.app.noTitle
import com.pyamsoft.pydroid.ui.app.requireArguments
import javax.inject.Inject

class PinConfirmDialog : DialogFragment(),
    Callback,
    PinConfirmUiComponent.Callback {

  @field:Inject internal lateinit var toolbarComponent: PinToolbarUiComponent
  @field:Inject internal lateinit var component: PinConfirmUiComponent

  private var finishOnDismiss: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true
    finishOnDismiss = requireArguments().getBoolean(
        FINISH_ON_DISMISS, false)
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    dialog.window?.apply {
      setLayout(
          LayoutParams.MATCH_PARENT,
          LayoutParams.WRAP_CONTENT
      )
      setGravity(Gravity.CENTER)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(layout.layout_constraint, container, false)
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

    val layoutRoot = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<PadLockComponent>(view.context.applicationContext)
        .plusPinComponent()
        .owner(viewLifecycleOwner)
        .parent(layoutRoot)
        .finishOnDismiss(finishOnDismiss)
        .build()
        .inject(this)

    component.bind(viewLifecycleOwner, savedInstanceState, this)
    toolbarComponent.bind(viewLifecycleOwner, savedInstanceState, this)

    toolbarComponent.layout(layoutRoot)
    component.layout(layoutRoot, toolbarComponent.id())
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    component.saveState(outState)
    toolbarComponent.saveState(outState)
  }

  override fun onDismiss(dialog: DialogInterface?) {
    super.onDismiss(dialog)
    if (finishOnDismiss) {
      activity?.finish()
    }
  }

  override fun onClose() {
    dismiss()
  }

  override fun onAttemptSubmit() {
    component.submit()
  }

  companion object {

    const val TAG = "PinConfirmDialog"
    private const val FINISH_ON_DISMISS = "finish_dismiss"

    @JvmStatic
    @CheckResult
    fun newInstance(finishOnDismiss: Boolean): PinConfirmDialog {
      return PinConfirmDialog()
          .apply {
        arguments = Bundle().apply {
          putBoolean(FINISH_ON_DISMISS, finishOnDismiss)
        }
      }
    }
  }
}

