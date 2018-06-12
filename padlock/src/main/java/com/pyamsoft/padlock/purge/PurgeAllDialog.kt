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

package com.pyamsoft.padlock.purge

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.model.PurgeAllEvent
import com.pyamsoft.padlock.uicommon.CanaryDialog
import javax.inject.Inject

class PurgeAllDialog : CanaryDialog() {

  @field:Inject
  internal lateinit var purgePublisher: PurgePublisher

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .inject(this)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireActivity())
        .setMessage("Really delete all old entries?")
        .setPositiveButton("Delete") { _, _ ->
          purgePublisher.publish(PurgeAllEvent)
          dismiss()
        }
        .setNegativeButton("Cancel") { _, _ -> dismiss() }
        .create()
  }
}
