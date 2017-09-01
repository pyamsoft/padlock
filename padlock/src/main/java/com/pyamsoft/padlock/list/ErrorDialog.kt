/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.list

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.presenter.Presenter

class ErrorDialog : CanaryDialog() {

  override fun provideBoundPresenters(): List<Presenter< *>> = emptyList()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity).setTitle("ERROR")
        .setMessage(
            "An unrecoverable error has occurred when attempting an operation. Please close and re-open PadLock")
        .setPositiveButton("Okay") { _, _ ->
          dismiss()
          activity.finish()
        }
        .setCancelable(false)
        .create()
  }
}
