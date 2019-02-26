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
import android.view.ViewGroup.LayoutParams
import androidx.annotation.CheckResult
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.app.noTitle
import com.pyamsoft.pydroid.ui.app.requireArguments
import javax.inject.Inject

class PinConfirmDialog : DialogFragment(),
    ConfirmPinPresenter.Callback,
    PinConfirmDialogPresenter.Callback {

  @field:Inject internal lateinit var pinView: ConfirmPinView
  @field:Inject internal lateinit var presenter: PinConfirmDialogPresenter
  @field:Inject internal lateinit var confirmPresenter: ConfirmPinPresenter

  private var finishOnDismiss: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true

    finishOnDismiss = requireArguments().getBoolean(FINISH_ON_DISMISS, false)
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
    val root = inflater.inflate(R.layout.layout_frame, container, false)
    val layoutRoot = root.findViewById<ViewGroup>(R.id.layout_frame)

    Injector.obtain<PadLockComponent>(root.context.applicationContext)
        .plusPinComponent()
        .owner(viewLifecycleOwner)
        .parent(layoutRoot)
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

    presenter.bind(viewLifecycleOwner, this)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    pinView.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    pinView.teardown()
  }

  override fun onDismiss(dialog: DialogInterface?) {
    super.onDismiss(dialog)
    if (finishOnDismiss) {
      requireActivity().finish()
    }
  }

  override fun onAttemptSubmit(attempt: String) {
    confirmPresenter.confirm(attempt)
  }

  override fun onConfirmPinBegin() {
    pinView.disable()
  }

  override fun onConfirmPinSuccess() {
    onPinCallback()
  }

  override fun onConfirmPinFailure() {
    onPinCallback()
  }

  override fun onConfirmPinComplete() {
    pinView.enable()
  }

  private fun onPinCallback() {
    pinView.clearDisplay()
    dismiss()
  }

  companion object {

    const val TAG = "PinConfirmDialog"
    private const val FINISH_ON_DISMISS = "finish_dismiss"

    @JvmStatic
    @CheckResult
    fun newInstance(finishOnDismiss: Boolean): PinConfirmDialog {
      return PinConfirmDialog().apply {
        arguments = Bundle().apply {
          putBoolean(FINISH_ON_DISMISS, finishOnDismiss)
        }
      }
    }
  }
}

