package com.pyamsoft.padlock.purge

interface PurgeItemView {

  fun bind(model: String)

  fun unbind()
}