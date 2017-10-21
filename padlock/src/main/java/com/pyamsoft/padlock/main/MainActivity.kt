/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.main

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.preference.PreferenceManager
import android.view.MenuItem
import com.pyamsoft.padlock.BuildConfig
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityMainBinding
import com.pyamsoft.padlock.main.MainPresenter.MainCallback
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment
import com.pyamsoft.pydroid.ui.sec.TamperActivity
import com.pyamsoft.pydroid.ui.util.AnimUtil
import com.pyamsoft.pydroid.util.AppUtil
import timber.log.Timber
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : TamperActivity(), MainPresenter.View {

  @Inject internal lateinit var presenter: MainPresenter
  private lateinit var binding: ActivityMainBinding

  override val currentApplicationVersion: Int = BuildConfig.VERSION_CODE

  override val safePackageName: String = "com.pyamsoft.padlock"

  override val versionName: String = BuildConfig.VERSION_NAME

  override val applicationIcon: Int = R.mipmap.ic_launcher

  override val applicationName: String by lazy(NONE) { getString(R.string.app_name) }

  override fun provideBoundPresenters(): List<Presenter<*>> =
      listOf(presenter) + super.provideBoundPresenters()

  override val changeLogLines: Array<String>
    get() = arrayOf(
        "BUGFIX: Fixed a crash on the lock screen when switching too quickly",
        "BUGFIX: Faster application startup"
    )

  public override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_PadLock_Light)
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)

    Injector.obtain<PadLockComponent>(applicationContext).inject(this)

    setAppBarState()

    presenter.bind(this)
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
    // TODO for now this is duplicated
    onShowDefaultPage()
  }


  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
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
}

