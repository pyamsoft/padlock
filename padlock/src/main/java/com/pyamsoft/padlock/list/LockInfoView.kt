package com.pyamsoft.padlock.list

import android.os.Bundle
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface LockInfoView : BaseScreen {

  fun onToolbarNavigationClicked(onClick: () -> Unit)

  fun onToolbarMenuItemClicked(onClick: (id: Int) -> Unit)

  fun onSwipeRefresh(onSwipe: () -> Unit)

  @CheckResult
  fun getListData(): List<ActivityEntry>

  fun commitListState(outState: Bundle?)

  fun onListPopulateBegin()

  fun onListPopulated()

  fun onListLoaded(list: List<ActivityEntry>)

  fun onListPopulateError(onAction: () -> Unit)

  fun onModifyEntryError(onAction: () -> Unit)

  fun onDatabaseChangeError(onAction: () -> Unit)

  fun onDatabaseChangeReceived(
    index: Int,
    entry: ActivityEntry
  )
}
