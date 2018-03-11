/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.helper

import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import com.pyamsoft.pydroid.cache.Cache

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
