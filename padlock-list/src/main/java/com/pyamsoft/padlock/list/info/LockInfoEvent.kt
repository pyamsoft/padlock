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

package com.pyamsoft.padlock.list.info

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState

sealed class LockInfoEvent {

    data class Modify internal constructor(val id: String, val name: String,
            val packageName: String,
            val oldState: LockState, val newState: LockState,
            val code: String?, val system: Boolean) : LockInfoEvent() {

        companion object {

            @JvmStatic
            @CheckResult
            fun from(entry: ActivityEntry, newState: LockState, code: String?,
                    system: Boolean): Modify {
                return Modify(id = entry.id, name = entry.name, packageName = entry.packageName,
                        oldState = entry.lockState, newState = newState, code = code,
                        system = system)
            }
        }
    }

    sealed class Callback : LockInfoEvent() {

        data class Created(val id: String, val packageName: String,
                val oldState: LockState) : Callback()

        data class Deleted(val id: String, val packageName: String,
                val oldState: LockState) : Callback()

        data class Whitelisted(val id: String, val packageName: String,
                val oldState: LockState) : Callback()

        data class Error(val throwable: Throwable, val packageName: String) : Callback()
    }
}
