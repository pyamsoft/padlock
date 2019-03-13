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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.R.layout
import com.pyamsoft.pydroid.ui.app.noTitle
import com.pyamsoft.pydroid.ui.app.requireArguments
import javax.inject.Inject

class PinConfirmDialog : DialogFragment(),
    ConfirmPinPresenter.Callback,
    PinToolbarPresenter.Callback,
    PinConfirmDialogPresenter.Callback {

  @field:Inject internal lateinit var toolbar: PinToolbar
  @field:Inject internal lateinit var pinView: ConfirmPinView

  @field:Inject internal lateinit var presenter: PinConfirmDialogPresenter
  @field:Inject internal lateinit var toolbarPresenter: PinToolbarPresenter
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
        .build()
        .inject(this)

    createComponents(savedInstanceState)
    layoutComponents(layoutRoot)

    confirmPresenter.bind(this)
    toolbarPresenter.bind(this)
    presenter.bind(this)
  }

  private fun createComponents(savedInstanceState: Bundle?) {
    toolbar.inflate(savedInstanceState)
    pinView.inflate(savedInstanceState)
  }

  private fun layoutComponents(layoutRoot: ConstraintLayout) {
    ConstraintSet().apply {
      clone(layoutRoot)

      toolbar.also {
        connect(it.id(), ConstraintSet.TOP, layoutRoot.id, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, layoutRoot.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, layoutRoot.id, ConstraintSet.END)
      }

      pinView.also {
        connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.BOTTOM, layoutRoot.id, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, layoutRoot.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, layoutRoot.id, ConstraintSet.END)
      }

      applyTo(layoutRoot)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    pinView.saveState(outState)
    toolbar.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    toolbar.teardown()
    pinView.teardown()

    confirmPresenter.unbind()
    toolbarPresenter.unbind()
    presenter.unbind()
  }

  override fun onDismiss(dialog: DialogInterface?) {
    super.onDismiss(dialog)
    if (finishOnDismiss) {
      activity?.finish()
    }
  }

  override fun onDialogClosed() {
    dismiss()
  }

  override fun onAttemptSubmit(attempt: String) {
    confirmPresenter.confirm(attempt, checkOnly = finishOnDismiss)
  }

  override fun onAttemptSubmit() {
    pinView.submit()
  }

  override fun onConfirmPinBegin() {
    pinView.disable()
  }

  override fun onConfirmPinSuccess(
    attempt: String,
    checkOnly: Boolean
  ) {
    onPinCallback()
  }

  override fun onConfirmPinFailure(
    attempt: String,
    checkOnly: Boolean
  ) {
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

