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
import com.pyamsoft.backstack.BackStack
import com.pyamsoft.padlock.BuildConfig
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityMainBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment
import com.pyamsoft.pydroid.ui.helper.DebouncedOnClickListener
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
                "FEATURE: Show indicator on main list if an application has whitelisted (never locked) or blacklisted (always locked) screens",
                "FEATURE: Show on the info dialog which screens are whitelisted or blacklisted",
                "BUGFIX: Explain blacklisting and whitelisting",
                "BUGFIX: Faster list fetching, do not clear static items"
        )

    private lateinit var backstack: BackStack

    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_PadLock_Light)
        super.onCreate(savedInstanceState)
        backstack = BackStack.create(this, R.id.fragment_container)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)

        Injector.obtain<PadLockComponent>(applicationContext).inject(this)

        setupToolbar()

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
            backstack.set(MainFragment.TAG) { MainFragment() }
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

    private fun setupToolbar() {
        binding.toolbar.apply {
            setToolbar(this)
            setTitle(R.string.app_name)
            ViewCompat.setElevation(this, AppUtil.convertToDP(context, 4f))

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

    override fun onBackPressed() {
        if (!backstack.back()) {
            super.onBackPressed()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        AnimUtil.animateActionBarToolbar(binding.toolbar)

        // Try to start service, will not if we do not have permission
        PadLockService.start(this)
    }
}

