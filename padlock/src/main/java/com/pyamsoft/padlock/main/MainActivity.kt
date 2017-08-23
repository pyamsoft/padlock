/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.main

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.preference.PreferenceManager
import android.view.MenuItem
import com.pyamsoft.padlock.BuildConfig
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityMainBinding
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment
import com.pyamsoft.pydroid.ui.sec.TamperActivity
import com.pyamsoft.pydroid.ui.util.AnimUtil
import com.pyamsoft.pydroid.util.AppUtil
import timber.log.Timber
import javax.inject.Inject

class MainActivity : TamperActivity(), MainPresenter.Callback {

  @Inject internal lateinit var presenter: MainPresenter
  private lateinit var binding: ActivityMainBinding

  public override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_PadLock_Light)
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)

    Injector.with(this) {
      it.inject(this)
    }

    setAppBarState()
  }

  override fun onStart() {
    super.onStart()
    presenter.start(this)
  }

  override fun onShowDefaultPage() {
    // Set normal navigation
    val fm = supportFragmentManager
    // Un hide the action bar in case it was hidden
    val actionBar = supportActionBar
    if (actionBar != null) {
      if (!actionBar.isShowing) {
        actionBar.show()
      }
    }

    if (fm.findFragmentByTag(MainFragment.TAG) == null && fm.findFragmentByTag(
        AboutLibrariesFragment.TAG) == null) {
      Timber.d("Load default page")
      fm.beginTransaction()
          .replace(R.id.fragment_container, MainFragment(), MainFragment.TAG)
          .commit()
    } else {
      Timber.w("Default page or About libraries was already loaded")
    }
  }

  override fun onShowOnboarding() {
    // TODO
    onShowDefaultPage()
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
  }

  override fun onPause() {
    super.onPause()
    if (isFinishing || isChangingConfigurations) {
      Timber.d(
          "Even though a leak is reported, this should dismiss the window, and clear the leak")
      binding.toolbar.menu.close()
      binding.toolbar.dismissPopupMenus()
    }
  }

  override val currentApplicationVersion: Int
    get() = BuildConfig.VERSION_CODE

  private fun setAppBarState() {
    setSupportActionBar(binding.toolbar)
    binding.toolbar.title = getString(R.string.app_name)
    ViewCompat.setElevation(binding.toolbar, AppUtil.convertToDP(this, 4f))
  }

  override fun onDestroy() {
    super.onDestroy()
    binding.unbind()
  }

  override fun onBackPressed() {
    val fragmentManager = supportFragmentManager
    val backStackCount = fragmentManager.backStackEntryCount
    if (backStackCount > 0) {
      fragmentManager.popBackStackImmediate()
    } else {
      super.onBackPressed()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val handled: Boolean
    when (item.itemId) {
      android.R.id.home -> {
        handled = true
        onBackPressed()
      }
      else -> handled = false
    }
    return handled
  }

  override fun onPostResume() {
    super.onPostResume()
    AnimUtil.animateActionBarToolbar(binding.toolbar)
  }

  override val safePackageName: String
    get() = "com.pyamsoft.padlock"

  override val changeLogLines: Array<String>
    get() {
      val line1 = "BUGFIX: Bugfixes and improvements"
      val line2 = "BUGFIX: Removed all Advertisements"
      val line3 = "BUGFIX: Faster loading of Open Source Licenses page"
      return arrayOf(line1, line2, line3)
    }

  override val versionName: String
    get() = BuildConfig.VERSION_NAME

  override val applicationIcon: Int
    get() = R.mipmap.ic_launcher

  override val applicationName: String
    get() = getString(R.string.app_name)
}

