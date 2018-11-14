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

package com.pyamsoft.padlock.settings

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import javax.inject.Inject

class ConfirmationDialog : ToolbarDialog() {

  @field:Inject
  internal lateinit var publisher: Publisher<ConfirmEvent>
  private lateinit var type: ConfirmEvent

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments!!.let {
      type = ConfirmEvent.valueOf(it.getString(WHICH, ConfirmEvent.DATABASE.name))
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .inject(this)

    return AlertDialog.Builder(requireActivity())
        .setMessage(
            if (type === ConfirmEvent.DATABASE)
              """Really clear entire database?
        |
        |You will have to re-configure all locked applications again""".trimMargin()
            else
              """Really clear all application settings?
        |
        |You will have to manually restart the Usage Access component of PadLock""".trimMargin()
        )
        .setPositiveButton("Yes") { _, _ ->
          publisher.publish(type)
          dismiss()
        }
        .setNegativeButton("No") { _, _ -> dismiss() }
        .create()
  }

  companion object {

    private const val WHICH = "which_type"

    @CheckResult
    @JvmStatic
    fun newInstance(type: ConfirmEvent): ConfirmationDialog {
      return ConfirmationDialog().apply {
        arguments = Bundle().apply {
          putString(WHICH, type.name)
        }
      }
    }
  }
}
