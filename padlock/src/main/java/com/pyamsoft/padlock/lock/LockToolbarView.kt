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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.lock.LockToolbarView.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

internal class LockToolbarView @Inject internal constructor(
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  override val layout: Int = R.layout.toolbar

  private val toolbar by lazyView<Toolbar>(R.id.toolbar)

  override fun id(): Int {
    return toolbar.id
  }

  @CheckResult
  fun isExcludeChecked(): Boolean {
    TODO()
  }

  @CheckResult
  fun getSelectedIgnoreTime(): Long {
    TODO()
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
  }

  override fun saveState(outState: Bundle) {
    super.saveState(outState)
    // TODO
  }

  interface Callback {

  }
}
