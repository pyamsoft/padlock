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

import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import com.pyamsoft.pydroid.data.Cache

object ListStateUtil : Cache {

  private const val KEY_CURRENT_POSITION: String = "key_current_position"
  private val cache: MutableMap<String, Int> = LinkedHashMap()

  @JvmStatic
  @CheckResult
  fun getCurrentPosition(recycler: RecyclerView?): Int {
    if (recycler == null) {
      return 0
    } else {
      val layoutManager: LayoutManager? = recycler.layoutManager
      if (layoutManager is LinearLayoutManager) {
        var position: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        if (position < 0) {
          position = layoutManager.findFirstVisibleItemPosition()
        }
        return position
      } else {
        return 0
      }
    }
  }

  @JvmStatic
  @CheckResult
  fun restoreState(
      tag: String,
      savedInstanceState: Bundle?
  ): Int {
    val position: Int = savedInstanceState?.getInt(KEY_CURRENT_POSITION, 0) ?: 0
    if (position == 0) {
      return cache[tag] ?: 0
    } else {
      return position
    }
  }

  @JvmStatic
  fun saveState(
      tag: String,
      outState: Bundle?,
      recycler: RecyclerView?
  ) {
    val position: Int = getCurrentPosition(recycler)
    if (position > 0) {
      outState?.putInt(KEY_CURRENT_POSITION, position)
      cache[tag] = position
    }
  }

  @JvmStatic
  @CheckResult
  fun restorePosition(
      lastPosition: Int,
      recycler: RecyclerView?
  ): Int {
    if (recycler != null) {
      if (lastPosition > 0) {
        recycler.adapter?.let {
          val size: Int = it.itemCount
          recycler.scrollToPosition(if (lastPosition > size) size - 1 else lastPosition)
        }
      }
    }

    return 0
  }

  override fun clearCache() {
    cache.clear()
  }
}
