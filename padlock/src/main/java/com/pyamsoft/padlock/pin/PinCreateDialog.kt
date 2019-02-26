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
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.R.layout
import com.pyamsoft.pydroid.ui.app.noTitle
import javax.inject.Inject

class PinCreateDialog : DialogFragment(),
    PinCreateDialogPresenter.Callback,
    CreatePinPresenter.Callback {

  @field:Inject internal lateinit var pinView: CreatePinView
  @field:Inject internal lateinit var presenter: PinCreateDialogPresenter
  @field:Inject internal lateinit var createPresenter: CreatePinPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true
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
    val root = inflater.inflate(layout.layout_frame, container, false)

    // TODO Inject

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

  override fun onAttemptSubmit(
    attempt: String,
    reEntry: String,
    hint: String
  ) {
    createPresenter.create(attempt, reEntry, hint)
  }

  override fun onCreatePinBegin() {
    pinView.disable()
  }

  override fun onCreatePinSuccess() {
    onPinCreateCallback()
  }

  override fun onCreatePinFailure() {
    onPinCreateCallback()
  }

  override fun onCreatePinComplete() {
    pinView.enable()
  }

  private fun onPinCreateCallback() {
    pinView.clearDisplay()
    dismiss()
  }

  companion object {

    const val TAG = "PinCreateDialog"
  }
}