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

package com.pyamsoft.padlock.purge

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.app.AlertDialog
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.uicommon.CanaryDialog
import javax.inject.Inject

class PurgeSingleItemDialog : CanaryDialog() {

  @field:Inject internal lateinit var purgeBus: PurgeBus
  private lateinit var packageName: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    packageName = arguments.getString(PACKAGE, "")

    Injector.with(context) {
      it.inject(this)
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity).setMessage("Really delete old entry for $packageName?")
        .setPositiveButton("Delete") { _, _ ->
          purgeBus.publish(PurgeEvent.create(packageName))
          dismiss()
        }
        .setNegativeButton("Cancel") { _, _ -> dismiss() }
        .create()
  }

  companion object {

    const private val PACKAGE = "package_name"

    @JvmStatic @CheckResult fun newInstance(packageName: String): PurgeSingleItemDialog {
      val args = Bundle()
      val fragment = PurgeSingleItemDialog()
      args.putString(PACKAGE, packageName)
      fragment.arguments = args
      return fragment
    }
  }
}
