package com.pyamsoft.padlock.purge

import android.os.Bundle
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface PurgeView : BaseScreen {

  @CheckResult
  fun getListModels(): List<String>

  fun onSwipeToRefresh(onSwipe: () -> Unit)

  fun onListItemClicked(onClick: (position: Int, model: String) -> Unit)

  fun onToolbarMenuItemClicked(onClick: (id: Int) -> Unit)

  fun onStaleFetchBegin(forced: Boolean)

  fun onStaleFetchSuccess(stalePackages: List<String>)

  fun onStaleFetchError(
    error: Throwable,
    onRetry: () -> Unit
  )

  fun onStaleFetchComplete()

  fun saveListPosition(outState: Bundle?)
}
