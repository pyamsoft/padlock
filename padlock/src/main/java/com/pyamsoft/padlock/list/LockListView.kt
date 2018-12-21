package com.pyamsoft.padlock.list

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface LockListView : BaseScreen {

  fun onRefreshed(onRefreshed: () -> Unit)

  fun onListItemClicked(onClick: (model: AppEntry) -> Unit)

  fun onToolbarMenuItemClicked(onClick: (item: MenuItem) -> Unit)

  fun onSwipeRefresh(onSwipe: () -> Unit)

  fun onFabClicked(onClick: () -> Unit)

  @CheckResult
  fun getListData(): List<AppEntry>

  fun commitListState(outState: Bundle?)

  fun onListPopulateBegin()

  fun onListPopulated()

  fun onListLoaded(list: List<AppEntry>)

  fun onListPopulateError(onAction: () -> Unit)

  fun onModifyEntryError(onAction: () -> Unit)

  fun onDatabaseChangeError(onAction: () -> Unit)

  fun onDatabaseChangeReceived(
    index: Int,
    entry: AppEntry
  )

  fun onSystemVisibilityChanged(visible: Boolean)

  fun onFabIconLocked()

  fun onFabIconUnlocked()

  fun onFabIconPermissionDenied()

  fun onFabIconPaused()

  fun onMasterPinCreateSuccess()

  fun onMasterPinCreateFailure()

  fun onMasterPinClearSuccess()

  fun onMasterPinClearFailure()
}
