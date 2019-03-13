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

package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.PinToolbar.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

internal class PinToolbar @Inject internal constructor(
  private val theming: Theming,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  override val layout: Int = R.layout.dark_toolbar

  override val layoutRoot by lazyView<Toolbar>(R.id.toolbar)

  private val submitItem by lazyMenuItem(R.id.menu_submit_pin)

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
    setupToolbar()
    inflateMenu()
  }

  private fun setupToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.ThemeOverlay_PadLock_Dark_Lock
    } else {
      theme = R.style.ThemeOverlay_PadLock_Light_Lock
    }
    layoutRoot.popupTheme = theme
    ViewCompat.setElevation(layoutRoot, 0f)

    layoutRoot.setNavigationOnClickListener(DebouncedOnClickListener.create {
      callback.onNavClicked()
    })
    layoutRoot.setUpEnabled(true)

    layoutRoot.title = "PIN"
  }

  private fun inflateMenu() {
    layoutRoot.inflateMenu(R.menu.pin_menu)
    val color: Int
    if (theming.isDarkTheme()) {
      color = R.color.white
    } else {
      color = R.color.black
    }
    submitItem.icon = submitItem.icon.tintWith(layoutRoot.context, color)

    submitItem.setOnMenuItemClickListener {
      callback.onSubmitClicked()
      return@setOnMenuItemClickListener true
    }
  }

  override fun onTeardown() {
    layoutRoot.setNavigationOnClickListener(null)
    submitItem.setOnMenuItemClickListener(null)
  }

  @CheckResult
  private fun lazyMenuItem(@IdRes id: Int): Lazy<MenuItem> {
    return lazy(NONE) { layoutRoot.menu.findItem(id) }
  }

  interface Callback {

    fun onNavClicked()

    fun onSubmitClicked()

  }
}

