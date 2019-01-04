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
