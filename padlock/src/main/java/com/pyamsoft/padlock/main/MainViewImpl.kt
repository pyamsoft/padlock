/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.main

import android.view.View
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityMainBinding
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

internal class MainViewImpl @Inject internal constructor(
  private val activity: MainActivity,
  private val theming: Theming
) : MainView {

  private lateinit var binding: ActivityMainBinding

  override fun create() {
    binding = DataBindingUtil.setContentView(activity, R.layout.activity_main)

    setupToolbar()
  }

  override fun root(): View {
    // Don't expose the real root here, just the fragment container
    return binding.fragmentContainer
  }

  override fun closeToolbarMenu() {
    Timber.d("A leak is reported, but this should dismiss the window, and clear the leak")
    binding.toolbar.apply {
      menu.close()
      dismissPopupMenus()
    }
  }

  private fun setupToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.ThemeOverlay_AppCompat
    } else {
      theme = R.style.ThemeOverlay_AppCompat_Light
    }

    binding.toolbar.apply {
      popupTheme = theme
      setTitle(R.string.app_name)
      ViewCompat.setElevation(this, 4f.toDp(context).toFloat())

      activity.setToolbar(this)
    }
  }

  override fun onToolbarNavigationClicked(onClick: () -> Unit) {
    binding.toolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
      onClick()
    })
  }

}
