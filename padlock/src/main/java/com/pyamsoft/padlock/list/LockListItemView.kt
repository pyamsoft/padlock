package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.model.list.AppEntry

interface LockListItemView {

  fun bind(model: AppEntry)

  fun unbind()

  fun onSwitchChanged(onChange: (isChecked: Boolean) -> Unit)

}
