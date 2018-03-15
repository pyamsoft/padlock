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

import android.support.v7.util.ListUpdateCallback
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.pydroid.list.ListDiffResult

fun <T : Any> ListDiffResult.ListData<T>.dispatch(adapter: ModelAdapter<*, *>) {
  this.dispatch(object : ListUpdateCallback {

    // If adapter.fastAdapter is null, the lifecycle is wrong.

    override fun onChanged(
      position: Int,
      count: Int,
      payload: Any?
    ) {
      adapter.fastAdapter!!.notifyAdapterItemRangeChanged(position, count, payload)
    }

    override fun onMoved(
      fromPosition: Int,
      toPosition: Int
    ) {
      adapter.fastAdapter!!.notifyAdapterItemMoved(fromPosition, toPosition)
    }

    override fun onInserted(
      position: Int,
      count: Int
    ) {
      adapter.fastAdapter!!.notifyAdapterItemRangeInserted(position, count)
    }

    override fun onRemoved(
      position: Int,
      count: Int
    ) {
      adapter.fastAdapter!!.notifyAdapterItemRangeRemoved(position, count)
    }
  })
}

