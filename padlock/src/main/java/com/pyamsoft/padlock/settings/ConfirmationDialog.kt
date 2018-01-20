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

package com.pyamsoft.padlock.settings

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.app.AlertDialog
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.uicommon.CanaryDialog
import javax.inject.Inject

class ConfirmationDialog : CanaryDialog() {

    @field:Inject
    internal lateinit var publisher: SettingsPublisher
    private lateinit var type: ConfirmEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = ConfirmEvent.valueOf(it.getString(WHICH, ConfirmEvent.DATABASE.name))
        }

        Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!).setMessage(
            if (type === ConfirmEvent.DATABASE)
                """Really clear entire database?
        |
        |You will have to re-configure all locked applications again""".trimMargin()
            else
                """Really clear all application settings?
        |
        |You will have to manually restart the Accessibility Service component of PadLock""".trimMargin()
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
