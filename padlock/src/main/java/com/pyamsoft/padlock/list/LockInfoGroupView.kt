package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.model.list.ActivityEntry

interface LockInfoGroupView {

  fun bind(
    model: ActivityEntry.Group,
    packageName: String
  )

  fun unbind()
}
