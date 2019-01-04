/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
