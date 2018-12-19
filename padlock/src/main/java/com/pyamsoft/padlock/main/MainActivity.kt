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

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.pyamsoft.padlock.BuildConfig
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityMainBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.service.ServiceManager
import com.pyamsoft.pydroid.ui.about.AboutFragment
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

class MainActivity : RatingActivity() {

  private lateinit var binding: ActivityMainBinding

  @field:Inject internal lateinit var serviceManager: ServiceManager
  @field:Inject internal lateinit var theming: Theming

  override val versionName: String
    get() = BuildConfig.VERSION_NAME

  override val applicationIcon: Int
    get() = R.mipmap.ic_launcher

  override val rootView: View
    get() = binding.fragmentContainer

  override val changeLogLines: ChangeLogBuilder
    get() =
      buildChangeLog {
        change("New icon style")
        change("Better open source license viewing experience")
        bugfix("Lots of tiny optimizations for locking applications.")
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.obtain<PadLockComponent>(applicationContext)
        .inject(this)

    if (theming.isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Normal)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Normal)
    }

    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

    setupToolbar()

    showDefaultPage()
  }

  private fun showDefaultPage() {
    // Set normal navigation
    val fm = supportFragmentManager

    if (fm.findFragmentByTag(MainFragment.TAG) == null && !AboutFragment.isPresent(this)) {
      Timber.d("Load default page")
      fm.beginTransaction()
          .add(R.id.fragment_container, MainFragment(), MainFragment.TAG)
          .commit(this)
    } else {
      Timber.w("Default page or About libraries was already loaded")
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onPause() {
    super.onPause()
    if (isFinishing || isChangingConfigurations) {
      Timber.d(
          "Even though a leak is reported, this should dismiss the window, and clear the leak"
      )
      binding.toolbar.menu.close()
      binding.toolbar.dismissPopupMenus()
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
      setToolbar(this)
      setTitle(R.string.app_name)
      ViewCompat.setElevation(this, 4f.toDp(context).toFloat())
      setNavigationOnClickListener(DebouncedOnClickListener.create {
        onBackPressed()
      })
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    binding.unbind()
    if (!isChangingConfigurations) {
      ListStateUtil.clearCache()
    }
  }

  override fun onPostResume() {
    super.onPostResume()
    // Try to start service, will not if we do not have permission
    serviceManager.startService(false)
  }
}
