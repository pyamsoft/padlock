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

package com.pyamsoft.padlock.purge

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.model.PurgeAllEvent
import com.pyamsoft.padlock.uicommon.CanaryDialog
import javax.inject.Inject

class PurgeAllDialog : CanaryDialog() {

    @field:Inject internal lateinit var purgePublisher: PurgePublisher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!).setMessage("Really delete all old entries?")
                .setPositiveButton("Delete") { _, _ ->
                    purgePublisher.publish(PurgeAllEvent)
                    dismiss()
                }
                .setNegativeButton("Cancel") { _, _ -> dismiss() }
                .create()
    }
}
