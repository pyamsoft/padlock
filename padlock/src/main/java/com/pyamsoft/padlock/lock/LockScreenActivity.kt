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

package com.pyamsoft.padlock.lock

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.CheckResult
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.loader.AppIconLoader
import com.pyamsoft.padlock.databinding.ActivityLockBinding
import com.pyamsoft.padlock.helper.isChecked
import com.pyamsoft.padlock.helper.setChecked
import com.pyamsoft.padlock.lock.screen.LockScreenPresenter
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.app.activity.DisposableActivity
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockScreenActivity : DisposableActivity(), LockScreenPresenter.View {

    private val home: Intent = Intent(Intent.ACTION_MAIN)
    @field:Inject internal lateinit var presenter: LockScreenPresenter
    @field:Inject internal lateinit var appIconLoader: AppIconLoader
    private lateinit var lockedActivityName: String
    private lateinit var lockedPackageName: String
    private lateinit var binding: ActivityLockBinding
    private lateinit var ignoreTimes: LongArray
    private lateinit var lockedRealName: String
    private var lockedSystem: Boolean = false
    private var ignorePeriod: Long = -1
    private var excludeEntry: Boolean = false
    private var appIcon = LoaderHelper.empty()
    private var lockedCode: String? = null

    // These can potentially be unassigned in onSaveInstanceState, mark them nullable
    private var menuIgnoreNone: MenuItem? = null
    private var menuIgnoreOne: MenuItem? = null
    private var menuIgnoreFive: MenuItem? = null
    private var menuIgnoreTen: MenuItem? = null
    private var menuIgnoreFifteen: MenuItem? = null
    private var menuIgnoreTwenty: MenuItem? = null
    private var menuIgnoreThirty: MenuItem? = null
    private var menuIgnoreFourtyFive: MenuItem? = null
    private var menuIgnoreSixty: MenuItem? = null
    internal var menuExclude: MenuItem? = null

    override val shouldConfirmBackPress: Boolean = false

    override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

    init {
        home.addCategory(Intent.CATEGORY_HOME)
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @CheckResult
    internal fun getRootView(): ViewGroup = binding.activityLockScreen

    @CheckResult
    internal fun getIgnoreTimeFromSelectedIndex(): Long {
        var index: Int
        try {
            index = when {
                menuIgnoreNone.isChecked() -> 0
                menuIgnoreOne.isChecked() -> 1
                menuIgnoreFive.isChecked() -> 2
                menuIgnoreTen.isChecked() -> 3
                menuIgnoreFifteen.isChecked() -> 4
                menuIgnoreTwenty.isChecked() -> 5
                menuIgnoreThirty.isChecked() -> 6
                menuIgnoreFourtyFive.isChecked() -> 7
                menuIgnoreSixty.isChecked() -> 8
                else -> 0
            }
        } catch (e: NullPointerException) {
            Timber.w("NULL menu item, default to 0")
            index = 0
        }

        return ignoreTimes[index]
    }

    @CallSuper public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_PadLock_Light_Lock)
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_lock)
        PreferenceManager.setDefaultValues(applicationContext, R.xml.preferences, false)

        populateIgnoreTimes()
        getValuesFromBundle()
        setupActionBar()

        Injector.obtain<PadLockComponent>(applicationContext).plusLockScreenComponent(
                LockEntryModule(lockedPackageName, lockedActivityName, lockedRealName)).inject(this)

        presenter.bind(this)
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        ViewCompat.setElevation(binding.toolbar, 0f)
    }

    private fun populateIgnoreTimes() {
        val stringIgnoreTimes = applicationContext.resources.getStringArray(
                R.array.ignore_time_entries)
        ignoreTimes = LongArray(stringIgnoreTimes.size)
        for (i in stringIgnoreTimes.indices) {
            ignoreTimes[i] = java.lang.Long.parseLong(stringIgnoreTimes[i])
        }
    }

    private fun getValuesFromBundle() {
        intent.extras.let {
            lockedCode = it.getString(ENTRY_LOCK_CODE)
            lockedPackageName = it.getString(ENTRY_PACKAGE_NAME)
            lockedActivityName = it.getString(ENTRY_ACTIVITY_NAME)
            lockedRealName = it.getString(ENTRY_REAL_NAME)
            lockedSystem = it.getBoolean(ENTRY_IS_SYSTEM, false)
        }

        Timber.d("onCreate Lock screen: $lockedPackageName $lockedActivityName $lockedRealName")

        // Reload options
        invalidateOptionsMenu()
    }

    override fun onStart() {
        super.onStart()

        appIcon = LoaderHelper.unload(appIcon)
        appIcon = appIconLoader.forPackageName(lockedPackageName).into(binding.lockImage)

        invalidateOptionsMenu()
    }

    override fun setDisplayName(name: String) {
        Timber.d("Set toolbar name %s", name)
        binding.toolbar.title = name
        val bar = supportActionBar
        if (bar != null) {
            Timber.d("Set actionbar name %s", name)
            bar.title = name
        }
    }

    override fun onCloseOldReceived() {
        Timber.w("Close event received for this LockScreen: %s", this)
        ActivityCompat.finishAffinity(this)
    }

    private fun pushFragment(pushFragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag(LockScreenTextFragment.TAG)
        if (fragment == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.lock_screen_container, pushFragment, tag)
                    .commit()
        }
    }

    override fun onTypePattern() {
        pushFragment(
                LockScreenPatternFragment.newInstance(lockedPackageName, lockedActivityName,
                        lockedCode,
                        lockedRealName, lockedSystem), LockScreenPatternFragment.TAG)
    }

    override fun onTypeText() {
        pushFragment(
                LockScreenTextFragment.newInstance(lockedPackageName, lockedActivityName,
                        lockedCode,
                        lockedRealName, lockedSystem), LockScreenTextFragment.TAG)
    }

    override fun onStop() {
        super.onStop()
        appIcon = LoaderHelper.unload(appIcon)
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

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        applicationContext.startActivity(home)
    }

    @CallSuper override fun onDestroy() {
        super.onDestroy()
        Timber.d("Clear currently locked")
        binding.unbind()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
        Timber.d("Finish called, either from Us or from Outside")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Timber.d("onRestoreInstanceState")
        ignorePeriod = savedInstanceState.getLong(KEY_IGNORE_TIME, -1)
        excludeEntry = savedInstanceState.getBoolean(KEY_EXCLUDE, false)
        val lockScreenText: Fragment? = supportFragmentManager.findFragmentByTag(
                LockScreenTextFragment.TAG)
        if (lockScreenText is LockScreenTextFragment) {
            lockScreenText.onRestoreInstanceState(savedInstanceState)
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putLong(KEY_IGNORE_TIME, getIgnoreTimeFromSelectedIndex())
            putBoolean(KEY_EXCLUDE, menuExclude.isChecked())
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu")
        menu.let {
            menuInflater.inflate(R.menu.lockscreen_menu, it)
            menuIgnoreNone = it.findItem(R.id.menu_ignore_none)
            menuIgnoreOne = it.findItem(R.id.menu_ignore_one)
            menuIgnoreFive = it.findItem(R.id.menu_ignore_five)
            menuIgnoreTen = it.findItem(R.id.menu_ignore_ten)
            menuIgnoreFifteen = it.findItem(R.id.menu_ignore_fifteen)
            menuIgnoreTwenty = it.findItem(R.id.menu_ignore_twenty)
            menuIgnoreThirty = it.findItem(R.id.menu_ignore_thirty)
            menuIgnoreFourtyFive = it.findItem(R.id.menu_ignore_fourtyfive)
            menuIgnoreSixty = it.findItem(R.id.menu_ignore_sixty)
            menuExclude = it.findItem(R.id.menu_exclude)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menuExclude.setChecked(excludeEntry)
        presenter.createWithDefaultIgnoreTime()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onInitializeWithIgnoreTime(time: Long) {
        val apply: Long
        if (ignorePeriod == -1L) {
            apply = time
            Timber.d("No previous selection, load ignore time from preference")
        } else {
            Timber.d("ignore period: $ignorePeriod")
            apply = ignorePeriod
        }

        when (apply) {
            ignoreTimes[0] -> menuIgnoreNone.setChecked(true)
            ignoreTimes[1] -> menuIgnoreOne.setChecked(true)
            ignoreTimes[2] -> menuIgnoreFive.setChecked(true)
            ignoreTimes[3] -> menuIgnoreTen.setChecked(true)
            ignoreTimes[4] -> menuIgnoreFifteen.setChecked(true)
            ignoreTimes[5] -> menuIgnoreTwenty.setChecked(true)
            ignoreTimes[6] -> menuIgnoreThirty.setChecked(true)
            ignoreTimes[7] -> menuIgnoreFourtyFive.setChecked(true)
            ignoreTimes[8] -> menuIgnoreSixty.setChecked(true)
            else -> {
                Timber.e("No valid ignore time, initialize to None")
                menuIgnoreNone.setChecked(true)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        val itemId = item.itemId
        when (itemId) {
            R.id.menu_exclude -> item.isChecked = !item.isChecked
            R.id.menu_lockscreen_info -> DialogUtil.guaranteeSingleDialogFragment(this,
                    LockedStatDialog.newInstance(binding.toolbar.title.toString(),
                            lockedPackageName,
                            lockedActivityName, lockedRealName, lockedSystem,
                            binding.lockImage.drawable),
                    "info_dialog")
            else -> item.isChecked = true
        }
        return true
    }

    companion object {

        const private val KEY_IGNORE_TIME = "key_ignore_time"
        const private val KEY_EXCLUDE = "key_exclude"
        const val ENTRY_PACKAGE_NAME = "entry_packagename"
        const val ENTRY_ACTIVITY_NAME = "entry_activityname"
        const val ENTRY_REAL_NAME = "real_name"
        const val ENTRY_LOCK_CODE = "lock_code"
        const val ENTRY_IS_SYSTEM = "is_system"

        /**
         * Starts a LockScreenActivity instance
         */

        @JvmStatic
        fun start(context: Context, entry: PadLockEntry, realName: String) {
            val intent = Intent(context.applicationContext, LockScreenActivity::class.java).apply {
                putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, entry.packageName())
                putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, entry.activityName())
                putExtra(LockScreenActivity.ENTRY_LOCK_CODE, entry.lockCode())
                putExtra(LockScreenActivity.ENTRY_IS_SYSTEM, entry.systemApplication())
                putExtra(LockScreenActivity.ENTRY_REAL_NAME, realName)

                // Always set flags
                flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK

                // If we are not locking PadLock, do a little differently
                if (entry.packageName() != context.applicationContext.packageName) {
                    flags = flags or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
            }

            if (entry.whitelist()) {
                throw RuntimeException("Cannot launch LockScreen for whitelisted applications")
            }

            context.applicationContext.startActivity(intent)
        }
    }
}
