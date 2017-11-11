/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.list

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.uicommon.UsageAccessRequestDelegate
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.presenter.Presenter

class UsageAccessRequestDialog : CanaryDialog() {

  override fun provideBoundPresenters(): List<Presenter<*>> = emptyList()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    activity!!.let {
      return AlertDialog.Builder(it).setTitle("Enable PadLock Usage Access")
          .setMessage(R.string.explain_accessibility_service)
          .setPositiveButton("Let's Go") { _, _ ->
            UsageAccessRequestDelegate.launchUsageAccessActivity(it)
            dismiss()
          }
          .setNegativeButton("No Thanks") { _, _ -> dismiss() }
          .create()
    }
  }

}