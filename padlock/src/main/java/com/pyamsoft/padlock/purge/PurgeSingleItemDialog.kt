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
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.model.purge.PurgeEvent
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import javax.inject.Inject

class PurgeSingleItemDialog : ToolbarDialog() {

  @field:Inject internal lateinit var purgePublisher: Publisher<PurgeEvent>

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val packageName = requireArguments().getString(PACKAGE, "")

    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .inject(this)

    return AlertDialog.Builder(requireActivity())
        .setMessage("Really delete old entry for $packageName?")
        .setPositiveButton("Delete") { _, _ ->
          purgePublisher.publish(PurgeEvent(packageName))
          dismiss()
        }
        .setNegativeButton("Cancel") { _, _ -> dismiss() }
        .create()
  }

  companion object {

    private const val PACKAGE = "package_name"

    @CheckResult
    @JvmStatic
    fun newInstance(packageName: String): PurgeSingleItemDialog {
      return PurgeSingleItemDialog().apply {
        arguments = Bundle().apply {
          putString(PACKAGE, packageName)
        }
      }
    }
  }
}
