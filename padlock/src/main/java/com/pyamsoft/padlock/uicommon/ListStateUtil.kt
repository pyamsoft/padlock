/*
 * Copyright 2017 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.uicommon

import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager

object ListStateUtil {

  const private val KEY_CURRENT_POSITION: String = "key_current_position"

  @JvmStatic
  @CheckResult
  fun getCurrentPosition(recycler: RecyclerView?): Int {
    return if (recycler == null) {
      // Return
      0
    } else {
      val layoutManager: LayoutManager? = recycler.layoutManager
      if (layoutManager is LinearLayoutManager) {
        // Return
        layoutManager.findFirstCompletelyVisibleItemPosition()
      } else {
        // Return
        0
      }
    }
  }

  @JvmStatic
  @CheckResult
  fun restoreState(savedInstanceState: Bundle?): Int =
      savedInstanceState?.getInt(KEY_CURRENT_POSITION, 0) ?: 0

  @JvmStatic
  fun saveState(outState: Bundle?, recycler: RecyclerView?) {
    outState?.putInt(KEY_CURRENT_POSITION,
        getCurrentPosition(recycler))
  }

  @JvmStatic
  @CheckResult
  fun restorePosition(lastPosition: Int, recycler: RecyclerView?): Int {
    if (recycler != null) {
      if (lastPosition != 0) {
        val adapter: RecyclerView.Adapter<*>? = recycler.adapter
        if (adapter != null) {
          val size: Int = adapter.itemCount
          recycler.scrollToPosition(
              if (lastPosition > size) size - 1 else lastPosition)
        }
      }
    }

    return 0
  }
}

