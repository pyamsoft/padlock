package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.MenuItem
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface LockScreenView : BaseScreen {

  fun onToolbarNavigationClicked(onClick: () -> Unit)

  fun onMenuItemClicked(onClick: (item: MenuItem) -> Unit)

  fun setToolbarTitle(title: String)

  fun closeToolbar()

  fun saveState(outState: Bundle)

  fun initIgnoreTimeSelection(time: Long)

}
