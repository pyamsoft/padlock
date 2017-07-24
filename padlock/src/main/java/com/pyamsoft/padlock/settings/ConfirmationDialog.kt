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

package com.pyamsoft.padlock.settings

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.app.AlertDialog
import com.pyamsoft.padlock.uicommon.CanaryDialog

class ConfirmationDialog : CanaryDialog() {

  internal lateinit var type: ConfirmEvent.Type

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    type = ConfirmEvent.Type.valueOf(arguments.getString(WHICH, ConfirmEvent.Type.DATABASE.name))
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity).setMessage(if (type === ConfirmEvent.Type.DATABASE)
      "Really clear entire database?\n\nYou will have to re-configure all locked applications again"
    else
      "Really clear all application settings?\n\nYou will have to manually restart the Accessibility Service component of PadLock")
        .setPositiveButton("Yes") { _, _ ->
          dismiss()
          TODO()
          //EventBus.get().publish(ConfirmEvent.create(type));
        }
        .setNegativeButton("No") { _, _ -> dismiss() }
        .create()
  }

  companion object {

    const private val WHICH = "which_type"

    @JvmStatic @CheckResult fun newInstance(type: ConfirmEvent.Type): ConfirmationDialog {
      val fragment = ConfirmationDialog()
      val args = Bundle()
      args.putString(WHICH, type.name)
      fragment.arguments = args
      return fragment
    }
  }
}
