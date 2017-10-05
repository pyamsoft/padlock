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

package com.pyamsoft.padlock

import android.content.Context
import com.pyamsoft.pydroid.SimpleInjector

object Injector : SimpleInjector {

  override val name: String = "com.pyamsoft.padlock.INJECTOR"

  override fun obtain(context: Context): Any {
    val service: Any? = context.getSystemService(name)
    if (service == null) {
      throw IllegalStateException("Service returned for getSystemService(\"$name\" was NULL")
    } else {
      return service
    }
  }
}
