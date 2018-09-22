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
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.pyamsoft.padlock.BuildConfig
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityMainBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.service.ServiceManager
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment
import com.pyamsoft.pydroid.ui.bugreport.BugreportDialog
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.animateMenu
import com.pyamsoft.pydroid.util.tintWith
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

class MainActivity : RatingActivity() {

  private lateinit var binding: ActivityMainBinding

  @field:Inject internal lateinit var serviceManager: ServiceManager

  override val currentApplicationVersion: Int
    get() = BuildConfig.VERSION_CODE

  override val versionName: String
    get() = BuildConfig.VERSION_NAME

  override val applicationIcon: Int
    get() = R.mipmap.ic_launcher

  override val applicationName: String get() = getString(R.string.app_name)

  override val rootView: View
    get() = binding.fragmentContainer

  override val forceUpdateCheck: Boolean get() = BuildConfig.DEBUG

  override val changeLogLines: ChangeLogBuilder
    get() =
      buildChangeLog {
        feature("New version release: 3.0.0")
        change("Faster, more reliable locking of applications")
        change("More material feeling with a fresh coat of paint and some animations")
        feature("Search applications based on any letters in the name instead of exact matches")
        feature("Application info dialog is split into logical groupings")
        bugfix("Avoid spamming the lock screen for applications which quickly switch screens")
        bugfix("Reduced memory usage on the lock screen and licenses screen")
        bugfix("Logical work when locking and unlocking applications is guaranteed off of the UI")
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_PadLock_Light)
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)

    Injector.obtain<PadLockComponent>(applicationContext)
        .inject(this)
    setupToolbar()

    showDefaultPage()
  }

  private fun showDefaultPage() {
    // Set normal navigation
    val fm = supportFragmentManager

    if (fm.findFragmentByTag(MainFragment.TAG) == null && !AboutLibrariesFragment.isPresent(this)) {
      Timber.d("Load default page")
      fm.beginTransaction()
          .add(R.id.fragment_container, MainFragment(), MainFragment.TAG)
          .commit()
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
    binding.toolbar.apply {
      setToolbar(this)
      setTitle(R.string.app_name)
      ViewCompat.setElevation(this, 4f.toDp(context).toFloat())
      setNavigationOnClickListener(DebouncedOnClickListener.create {
        onBackPressed()
      })
    }

    BugreportDialog.attachToToolbar(
        this, binding.toolbar, applicationName, currentApplicationVersion
    )
    val bugReport = binding.toolbar.menu.findItem(R.id.menu_item_bugreport)
    val icon = bugReport.icon
    icon.mutate()
        .also {
          val tintedIcon = it.tintWith(this, R.color.black)
          bugReport.icon = tintedIcon
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
    binding.toolbar.animateMenu()

    // Try to start service, will not if we do not have permission
    serviceManager.startService(false)
  }
}
