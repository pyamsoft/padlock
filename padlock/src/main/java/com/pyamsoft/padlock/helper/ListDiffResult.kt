package com.pyamsoft.padlock.helper

import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.commons.utils.FastAdapterDiffUtil
import com.pyamsoft.pydroid.list.ListDiffResult

fun <M : Any, I : IItem<*, *>> ListDiffResult<M>.dispatch(adapter: ModelAdapter<M, I>) {
  this.ifEmpty { adapter.clear() }
  this.withValues { payload ->
    adapter.set(payload.list(), true) { _, _, _, _ -> false }
    payload.dispatch { FastAdapterDiffUtil.set(adapter, it) }
  }
}
