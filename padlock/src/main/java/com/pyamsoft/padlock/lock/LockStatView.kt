package com.pyamsoft.padlock.lock

import com.pyamsoft.pydroid.ui.app.BaseScreen

interface LockStatView : BaseScreen {

  fun setDisplayName(name: String)
}
