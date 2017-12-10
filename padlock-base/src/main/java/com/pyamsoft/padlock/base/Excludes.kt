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

package com.pyamsoft.padlock.base

object Excludes {

    private val PACKAGES: List<String> = listOf(
            "android",
            "com.android.systemui"
    )

    private val CLASSES: List<String> = listOf(
            "com.pyamsoft.padlock.lock.lockscreenactivity"
    )

    @JvmStatic
    fun isPackageExcluded(name: String): Boolean = checkExclusion(PACKAGES, name)

    @JvmStatic
    fun isClassExcluded(name: String): Boolean = checkExclusion(CLASSES, name)

    private fun checkExclusion(list: List<String>, name: String): Boolean {
        val check = name.trim().toLowerCase()
        return list.contains(check)
    }

    @JvmStatic
    fun isLockScreen(packageName: String, className: String): Boolean {
        val packageCheck = packageName.trim().toLowerCase()
        val classCheck = className.trim().toLowerCase()
        return (packageCheck == "com.pyamsoft.padlock"
                && classCheck == "com.pyamsoft.padlock.lock.lockscreenactivity")
    }
}

