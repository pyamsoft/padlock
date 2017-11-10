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
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.items.ModelAbstractItem

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

fun <M : Any> ModelAdapter<M, out ModelAbstractItem<M, *, *>>.retainAll(
    backing: MutableCollection<M>): Boolean {
  val old: MutableCollection<ModelAbstractItem<M, *, *>> = LinkedHashSet()
  adapterItems.filterNotTo(old) { backing.contains(it.model) }

  // Don't replace with stdlib operation, since we need the getAdapterPosition call
  // to happen on each new loop.
  @Suppress("LoopToCallChain")
  for (item in old) {
    val index = getAdapterPosition(item.identifier)
    if (index >= 0) {
      remove(index)
    }
  }

  backing.clear()
  return old.isNotEmpty()
}
