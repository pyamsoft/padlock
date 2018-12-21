package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.list.ActivityEntry

interface LockInfoItemView {

  fun bind(
    model: ActivityEntry.Item,
    system: Boolean
  )

  fun unbind()

  fun onSwitchChanged(onChange: (lockState: LockState) -> Unit)
}
