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

package com.pyamsoft.padlock.purge

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.pydroid.ui.app.requireArguments
import javax.inject.Inject

class PurgeAllDialog : DialogFragment() {

  @field:Inject internal lateinit var presenter: PurgeAllPresenter

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .inject(this)

    val packageNames = requireArguments().getStringArrayList(PACKAGES) ?: emptyList<String>()
    return AlertDialog.Builder(requireActivity())
        .setMessage("Really delete all old entries?")
        .setPositiveButton("Delete") { _, _ ->
          presenter.purge(packageNames)
          dismiss()
        }
        .setNegativeButton("Cancel") { _, _ -> dismiss() }
        .create()
  }

  companion object {

    private const val PACKAGES = "package_names"

    @CheckResult
    @JvmStatic
    fun newInstance(packageNames: List<String>): PurgeAllDialog {
      return PurgeAllDialog().apply {
        arguments = Bundle().apply {
          putStringArrayList(PACKAGES, ArrayList(packageNames))
        }
      }
    }
  }
}
