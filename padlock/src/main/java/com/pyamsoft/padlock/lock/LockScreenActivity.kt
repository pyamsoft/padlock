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

package com.pyamsoft.padlock.lock

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.CheckResult
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.databinding.ActivityLockBinding
import com.pyamsoft.padlock.lock.screen.LockScreenModule
import com.pyamsoft.padlock.lock.screen.LockScreenPresenter
import com.pyamsoft.padlock.lock.screen.LockScreenPresenter.NameCallback
import com.pyamsoft.padlock.uicommon.AppIconLoader
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.app.activity.ActivityBase
import com.pyamsoft.pydroid.ui.app.activity.DisposableActivity
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockScreenActivity : DisposableActivity(), LockScreenPresenter.Callback, LockScreenPresenter.NameCallback {

  private val home: Intent = Intent(Intent.ACTION_MAIN)
  @field:Inject internal lateinit var presenter: LockScreenPresenter
  private lateinit var lockedActivityName: String
  private lateinit var lockedPackageName: String
  private lateinit var binding: ActivityLockBinding
  private lateinit var menuIgnoreNone: MenuItem
  private lateinit var menuIgnoreOne: MenuItem
  private lateinit var menuIgnoreFive: MenuItem
  private lateinit var menuIgnoreTen: MenuItem
  private lateinit var menuIgnoreFifteen: MenuItem
  private lateinit var menuIgnoreTwenty: MenuItem
  private lateinit var menuIgnoreThirty: MenuItem
  private lateinit var menuIgnoreFourtyFive: MenuItem
  private lateinit var menuIgnoreSixty: MenuItem
  private lateinit var ignoreTimes: LongArray
  private lateinit var lockedRealName: String
  private var lockedSystem: Boolean = false
  private var ignorePeriod: Long = -1
  private var excludeEntry: Boolean = false
  private var appIcon = LoaderHelper.empty()
  private var lockedCode: String? = null
  internal lateinit var menuExclude: MenuItem

  override val shouldConfirmBackPress: Boolean
    get() = false

  override fun provideBoundPresenters(): List<Presenter<*, *>> = listOf(presenter)

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
        menuIgnoreNone.isChecked -> 0
        menuIgnoreOne.isChecked -> 1
        menuIgnoreFive.isChecked -> 2
        menuIgnoreTen.isChecked -> 3
        menuIgnoreFifteen.isChecked -> 4
        menuIgnoreTwenty.isChecked -> 5
        menuIgnoreThirty.isChecked -> 6
        menuIgnoreFourtyFive.isChecked -> 7
        menuIgnoreSixty.isChecked -> 8
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

    Injector.with(this) {
      it.plusLockScreenComponent(
          LockScreenModule(lockedPackageName, lockedActivityName),
          LockEntryModule(lockedPackageName, lockedActivityName, lockedRealName)).inject(this)
    }

    presenter.create(this)
  }

  private fun setupActionBar() {
    setSupportActionBar(binding.toolbar)
    ViewCompat.setElevation(binding.toolbar, 0f)
  }

  private fun populateIgnoreTimes() {
    val stringIgnoreTimes = applicationContext.resources.getStringArray(R.array.ignore_time_entries)
    ignoreTimes = LongArray(stringIgnoreTimes.size)
    for (i in stringIgnoreTimes.indices) {
      ignoreTimes[i] = java.lang.Long.parseLong(stringIgnoreTimes[i])
    }
  }

  private fun getValuesFromBundle() {
    val bundle = intent.extras
    lockedCode = bundle.getString(ENTRY_LOCK_CODE)
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME)
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME)
    lockedRealName = bundle.getString(ENTRY_REAL_NAME)
    lockedSystem = bundle.getBoolean(ENTRY_IS_SYSTEM, false)

    // Reload options
    invalidateOptionsMenu()
  }

  override fun onStart() {
    super.onStart()
    presenter.start(this)

    appIcon = LoaderHelper.unload(appIcon)
    appIcon = ImageLoader.fromLoader(AppIconLoader.forPackageName(this, lockedPackageName))
        .into(binding.lockImage)

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
    finish()
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
        LockScreenPatternFragment.newInstance(lockedPackageName, lockedActivityName, lockedCode,
            lockedRealName, lockedSystem), LockScreenPatternFragment.TAG)
  }

  override fun onTypeText() {
    pushFragment(
        LockScreenTextFragment.newInstance(lockedPackageName, lockedActivityName, lockedCode,
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
    ignorePeriod = savedInstanceState.getLong("IGNORE", -1)
    excludeEntry = savedInstanceState.getBoolean("EXCLUDE", false)
    val lockScreenText = supportFragmentManager.findFragmentByTag(LockScreenTextFragment.TAG)
    if (lockScreenText is LockScreenTextFragment?) {
      lockScreenText?.onRestoreInstanceState(savedInstanceState)
    }
    super.onRestoreInstanceState(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    val ignoreTime = getIgnoreTimeFromSelectedIndex()
    outState.putLong("IGNORE", ignoreTime)

    val exclude: Boolean = try {
      // Assigned to exclude
      menuExclude.isChecked
    } catch (e: NullPointerException) {
      // Assigned to exclude
      false
    }

    outState.putBoolean("EXCLUDE", exclude)
    super.onSaveInstanceState(outState)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    Timber.d("onCreateOptionsMenu")
    menuInflater.inflate(R.menu.lockscreen_menu, menu)
    menuIgnoreNone = menu.findItem(R.id.menu_ignore_none)
    menuIgnoreOne = menu.findItem(R.id.menu_ignore_one)
    menuIgnoreFive = menu.findItem(R.id.menu_ignore_five)
    menuIgnoreTen = menu.findItem(R.id.menu_ignore_ten)
    menuIgnoreFifteen = menu.findItem(R.id.menu_ignore_fifteen)
    menuIgnoreTwenty = menu.findItem(R.id.menu_ignore_twenty)
    menuIgnoreThirty = menu.findItem(R.id.menu_ignore_thirty)
    menuIgnoreFourtyFive = menu.findItem(R.id.menu_ignore_fourtyfive)
    menuIgnoreSixty = menu.findItem(R.id.menu_ignore_sixty)
    menuExclude = menu.findItem(R.id.menu_exclude)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menuExclude.isChecked = excludeEntry
    presenter.createWithDefaultIgnoreTime {
      val apply: Long
      if (ignorePeriod == -1L) {
        apply = it
        Timber.d("No previous selection, load ignore time from preference")
      } else {
        Timber.d("ignore period: $ignorePeriod")
        apply = ignorePeriod
      }

      when (apply) {
        ignoreTimes[0] -> menuIgnoreNone.isChecked = true
        ignoreTimes[1] -> menuIgnoreOne.isChecked = true
        ignoreTimes[2] -> menuIgnoreFive.isChecked = true
        ignoreTimes[3] -> menuIgnoreTen.isChecked = true
        ignoreTimes[4] -> menuIgnoreFifteen.isChecked = true
        ignoreTimes[5] -> menuIgnoreTwenty.isChecked = true
        ignoreTimes[6] -> menuIgnoreThirty.isChecked = true
        ignoreTimes[7] -> menuIgnoreFourtyFive.isChecked = true
        ignoreTimes[8] -> menuIgnoreSixty.isChecked = true
        else -> {
          Timber.e("No valid ignore time, initialize to None")
          menuIgnoreNone.isChecked = true
        }
      }
    }

    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    Timber.d("onOptionsItemSelected")
    val itemId = item.itemId
    when (itemId) {
      R.id.menu_exclude -> item.isChecked = !item.isChecked
      R.id.menu_lockscreen_info -> DialogUtil.guaranteeSingleDialogFragment(this,
          LockedStatDialog.newInstance(binding.toolbar.title.toString(), lockedPackageName,
              lockedActivityName, lockedRealName, lockedSystem, binding.lockImage.drawable),
          "info_dialog")
      else -> item.isChecked = true
    }
    return true
  }

  companion object {

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
      val intent = Intent(context.applicationContext, LockScreenActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
      intent.putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, entry.packageName())
      intent.putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, entry.activityName())
      intent.putExtra(LockScreenActivity.ENTRY_LOCK_CODE, entry.lockCode())
      intent.putExtra(LockScreenActivity.ENTRY_IS_SYSTEM, entry.systemApplication())
      intent.putExtra(LockScreenActivity.ENTRY_REAL_NAME, realName)

      if (entry.whitelist()) {
        throw RuntimeException("Cannot launch LockScreen for whitelisted applications")
      }

      context.applicationContext.startActivity(intent)
    }
  }
}
