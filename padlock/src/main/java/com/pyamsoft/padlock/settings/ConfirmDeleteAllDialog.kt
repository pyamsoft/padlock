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

package com.pyamsoft.padlock.settings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import javax.inject.Inject

class ConfirmDeleteAllDialog : DialogFragment() {

  @field:Inject internal lateinit var presenter: ClearAllPresenter

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusConfirmationComponent()
        .owner(viewLifecycleOwner)
        .build()
        .inject(this)

    return AlertDialog.Builder(requireActivity())
        .setMessage(
            """
              |Really clear all application settings?
              |
              |You will have to manually restart the Usage Access component of PadLock
              |""".trimMargin()
        )
        .setPositiveButton("Yes") { _, _ ->
          presenter.clear()
          dismiss()
        }
        .setNegativeButton("No") { _, _ -> dismiss() }
        .create()
  }
}
