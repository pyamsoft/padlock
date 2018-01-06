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

package com.pyamsoft.padlock.lock.passed

import com.pyamsoft.padlock.api.LockPassed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class LockPassedImpl @Inject internal constructor() :
        LockPassed {

    private val passedSet: MutableCollection<String> = LinkedHashSet()

    override fun add(packageName: String, activityName: String) {
        passedSet.add("$packageName$activityName")
    }

    override fun remove(packageName: String, activityName: String) {
        passedSet.remove("$packageName$activityName")
    }

    override fun check(packageName: String, activityName: String): Boolean =
            passedSet.contains("$packageName$activityName")
}
