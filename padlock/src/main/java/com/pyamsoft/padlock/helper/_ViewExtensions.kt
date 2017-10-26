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

package com.pyamsoft.padlock.helper

import android.support.annotation.CheckResult
import android.support.v4.widget.SwipeRefreshLayout
import android.view.MenuItem
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.items.GenericAbstractItem

@CheckResult
fun MenuItem?.isChecked(): Boolean = this != null && isChecked

fun MenuItem?.setChecked(checked: Boolean) {
  if (this != null) {
    isChecked = checked
  }
}

fun SwipeRefreshLayout.refreshing(refreshing: Boolean) {
  post { isRefreshing = refreshing }
}

fun <M : Any, T : GenericAbstractItem<M, *, *>> FastItemAdapter<T>.retainAll(
    items: Collection<M>): Boolean {
  val old: MutableSet<T> = LinkedHashSet()
  adapterItems.filterNotTo(old) { items.contains(it.model) }
  old.map { adapterItems.indexOf(it) }.filter { it >= 0 }.forEach { remove(it) }
  return old.isNotEmpty()
}
