package com.pyamsoft.padlock.pin

import com.pyamsoft.pydroid.ui.app.BaseScreen

interface PinView : BaseScreen {

  fun onToolbarNavigationClicked(onClick: () -> Unit)

  fun onToolbarMenuItemClicked(onClick: (id: Int) -> Unit)

}