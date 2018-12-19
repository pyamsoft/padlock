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

package com.pyamsoft.padlock.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityLockBinding
import com.pyamsoft.padlock.helper.isChecked
import com.pyamsoft.padlock.helper.setChecked
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.lock.screen.LockScreenViewModel
import com.pyamsoft.padlock.lock.screen.PinScreenInputViewModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.activity.ActivityBase
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class LockScreenActivity : ActivityBase() {

  @field:Inject internal lateinit var viewModel: LockScreenViewModel
  @field:Inject internal lateinit var inputViewModel: PinScreenInputViewModel
  @field:Inject internal lateinit var appIconLoader: AppIconLoader
  @field:Inject internal lateinit var theming: Theming

  private lateinit var lockedActivityName: String
  private lateinit var lockedPackageName: String
  private lateinit var binding: ActivityLockBinding
  private lateinit var ignoreTimes: MutableList<Long>
  private lateinit var lockedRealName: String

  private lateinit var lockScreenComponent: LockScreenComponent

  private val home: Intent = Intent(Intent.ACTION_MAIN)

  private var lockedSystem: Boolean = false
  private var ignorePeriod: Long = -1
  private var excludeEntry: Boolean = false
  private var lockedCode: String? = null
  private var lockedIcon: Int = 0

  // These can potentially be unassigned in onSaveInstanceState, mark them nullable
  private var menuIgnoreOne: MenuItem? = null
  private var menuIgnoreFive: MenuItem? = null
  private var menuIgnoreTen: MenuItem? = null
  private var menuIgnoreFifteen: MenuItem? = null
  private var menuIgnoreTwenty: MenuItem? = null
  private var menuIgnoreThirty: MenuItem? = null
  private var menuIgnoreFourtyFive: MenuItem? = null
  private var menuIgnoreSixty: MenuItem? = null
  private var menuExclude: MenuItem? = null

  private var alreadyUnlockedDisposable by singleDisposable()
  private var lockScreenTypeDisposable by singleDisposable()
  private var recreateDisposable by singleDisposable()
  private var displayNameDisposable by singleDisposable()
  private var ignoreTimeDisposable by singleDisposable()
  private var closeOldDisposable by singleDisposable()

  init {
    home.addCategory(Intent.CATEGORY_HOME)
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
  }

  @CheckResult
  internal fun isExcluded(): Boolean {
    return menuExclude.isChecked()
  }

  @CheckResult
  internal fun getRootView(): ViewGroup = binding.lockScreenContainer

  @CheckResult
  internal fun getIgnoreTimeFromSelectedIndex(): Long {
    var index: Int
    try {
      index = when {
        menuIgnoreOne.isChecked() -> 0
        menuIgnoreFive.isChecked() -> 1
        menuIgnoreTen.isChecked() -> 2
        menuIgnoreFifteen.isChecked() -> 3
        menuIgnoreTwenty.isChecked() -> 4
        menuIgnoreThirty.isChecked() -> 5
        menuIgnoreFourtyFive.isChecked() -> 6
        menuIgnoreSixty.isChecked() -> 7
        else -> 0
      }
    } catch (e: NullPointerException) {
      Timber.w("NULL menu item, default to 1")
      index = 1
    }

    return ignoreTimes[index]
  }

  override fun getSystemService(name: String): Any {
    if (Injector.name == name) {
      return lockScreenComponent
    } else {
      return super.getSystemService(name)
    }
  }

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    overridePendingTransition(0, 0)
    preInjectOnCreate()

    lockScreenComponent = Injector.obtain<PadLockComponent>(applicationContext)
        .plusLockScreenComponent()
        .owner(this)
        .packageName(lockedPackageName)
        .activityName(lockedActivityName)
        .realName(lockedRealName)
        .build()

    lockScreenComponent.inject(this)

    if (theming.isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Lock)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Lock)
    }
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_lock)

    postInjectOnCreate()


    checkIfAlreadyUnlocked()

    closeOldDisposable = viewModel.closeOld { onCloseOldReceived() }

    ignoreTimeDisposable = viewModel.createWithDefaultIgnoreTime { onInitializeWithIgnoreTime(it) }

    displayNameDisposable = viewModel.loadDisplayNameFromPackage { onSetDisplayName(it) }

    recreateDisposable = viewModel.onRecreateEvent { recreate() }

    lockScreenTypeDisposable = inputViewModel.resolveLockScreenType(
        onTypeText = { onTypeText() },
        onTypePattern = { onTypePattern() }
    )
  }

  override fun onResume() {
    super.onResume()
    checkIfAlreadyUnlocked()
  }

  private fun checkIfAlreadyUnlocked() {
    alreadyUnlockedDisposable = viewModel.checkIfAlreadyUnlocked { onAlreadyUnlocked() }
  }

  private fun preInjectOnCreate() {
    getValuesFromBundle()
  }

  private fun postInjectOnCreate() {
    appIconLoader.loadAppIcon(lockedPackageName, lockedIcon)
        .into(binding.lockImage)
        .bind(this)
    populateIgnoreTimes()
    setupToolbar()
    Timber.d("onCreate LockScreenActivity for $lockedPackageName $lockedRealName")
  }

  private fun setupToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.ThemeOverlay_PadLock_Dark_Lock
    } else {
      theme = R.style.ThemeOverlay_PadLock_Light_Lock
    }
    binding.toolbar.apply {
      popupTheme = theme

      setToolbar(this)
      ViewCompat.setElevation(this, 0f)
      setNavigationOnClickListener(
          DebouncedOnClickListener.create { onBackPressed() })

      inflateMenu(R.menu.lockscreen_menu)
      menu.let {
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
    }

    menuExclude.setChecked(excludeEntry)

    binding.toolbar.setOnMenuItemClickListener {
      val itemId = it.itemId
      when (itemId) {
        R.id.menu_exclude -> it.isChecked = !it.isChecked
        R.id.menu_lockscreen_info -> {
          LockedStatDialog.newInstance(
              binding.toolbar.title.toString(), lockedPackageName, lockedActivityName,
              lockedRealName, lockedSystem
          )
              .show(this, "info_dialog")
        }
        else -> it.isChecked = true
      }
      return@setOnMenuItemClickListener true
    }
  }

  private fun populateIgnoreTimes() {
    val stringIgnoreTimes = resources.getStringArray(R.array.ignore_time_entries)
    ignoreTimes = ArrayList(stringIgnoreTimes.size)
    for (i in stringIgnoreTimes.indices) {
      ignoreTimes.add(stringIgnoreTimes[i].toLong())
    }
  }

  private fun getValuesFromBundle() {
    requireNotNull(intent.extras).also {
      lockedCode = it.getString(ENTRY_LOCK_CODE)
      lockedPackageName = it.getString(ENTRY_PACKAGE_NAME, "")
      lockedActivityName = it.getString(ENTRY_ACTIVITY_NAME, "")
      lockedRealName = it.getString(ENTRY_REAL_NAME, "")
      lockedIcon = it.getInt(ENTRY_ICON, 0)
      lockedSystem = it.getBoolean(ENTRY_IS_SYSTEM, false)
    }

    require(lockedPackageName.isNotBlank())
    require(lockedActivityName.isNotBlank())
    require(lockedRealName.isNotBlank())
  }

  private fun onSetDisplayName(name: String) {
    binding.toolbar.title = name
  }

  private fun onAlreadyUnlocked() {
    Timber.d("$lockedPackageName $lockedActivityName is already unlocked")
    finish()
  }

  private fun onCloseOldReceived() {
    Timber.w("Close event received for this LockScreen: %s", this)
    finish()
  }

  private fun pushFragment(
    pushFragment: Fragment,
    tag: String
  ) {
    val fragmentManager = supportFragmentManager
    val fragment = fragmentManager.findFragmentByTag(LockScreenTextFragment.TAG)
    if (fragment == null) {
      fragmentManager.beginTransaction()
          .add(R.id.lock_screen_container, pushFragment, tag)
          .commit(this)
    }
  }

  private fun onTypePattern() {
    pushFragment(
        LockScreenPatternFragment.newInstance(
            lockedPackageName, lockedActivityName,
            lockedCode,
            lockedRealName, lockedSystem
        ), LockScreenPatternFragment.TAG
    )
  }

  private fun onTypeText() {
    pushFragment(
        LockScreenTextFragment.newInstance(
            lockedPackageName, lockedActivityName,
            lockedCode,
            lockedRealName, lockedSystem
        ), LockScreenTextFragment.TAG
    )
  }

  override fun onPause() {
    super.onPause()
    Timber.d("Pausing LockScreen $lockedPackageName $lockedRealName")
    if (isFinishing || isChangingConfigurations) {
      Timber.d(
          "Even though a leak is reported, this should dismiss the window, and clear the leak"
      )
      binding.toolbar.menu.close()
      binding.toolbar.dismissPopupMenus()
    }

    viewModel.clearMatchingForegroundEvent()
  }

  override fun onBackPressed() {
    applicationContext.startActivity(home)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    overridePendingTransition(0, 0)

    alreadyUnlockedDisposable.tryDispose()
    lockScreenTypeDisposable.tryDispose()
    recreateDisposable.tryDispose()
    displayNameDisposable.tryDispose()
    ignoreTimeDisposable.tryDispose()
    closeOldDisposable.tryDispose()

    binding.unbind()
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
    Timber.d("Finish called, either from Us or from Outside")
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    ignorePeriod = savedInstanceState.getLong(KEY_IGNORE_TIME, -1)
    excludeEntry = savedInstanceState.getBoolean(KEY_EXCLUDE, false)
    val lockScreenText: Fragment? =
      supportFragmentManager.findFragmentByTag(LockScreenTextFragment.TAG)
    if (lockScreenText is LockScreenBaseFragment) {
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

  private fun onInitializeWithIgnoreTime(time: Long) {
    val apply: Long
    if (ignorePeriod == -1L) {
      apply = time
      Timber.d("No previous selection, load ignore time from preference")
    } else {
      Timber.d("ignore period: $ignorePeriod")
      apply = ignorePeriod
    }

    when (apply) {
      ignoreTimes[0] -> menuIgnoreOne.setChecked(true)
      ignoreTimes[1] -> menuIgnoreFive.setChecked(true)
      ignoreTimes[2] -> menuIgnoreTen.setChecked(true)
      ignoreTimes[3] -> menuIgnoreFifteen.setChecked(true)
      ignoreTimes[4] -> menuIgnoreTwenty.setChecked(true)
      ignoreTimes[5] -> menuIgnoreThirty.setChecked(true)
      ignoreTimes[6] -> menuIgnoreFourtyFive.setChecked(true)
      ignoreTimes[7] -> menuIgnoreSixty.setChecked(true)
      else -> {
        Timber.e("No valid ignore time, initialize to None")
        menuIgnoreOne.setChecked(true)
      }
    }
  }

  companion object {

    private const val KEY_IGNORE_TIME = "key_ignore_time"
    private const val KEY_EXCLUDE = "key_exclude"
    internal const val ENTRY_PACKAGE_NAME = "entry_packagename"
    internal const val ENTRY_ACTIVITY_NAME = "entry_activityname"
    internal const val ENTRY_REAL_NAME = "real_name"
    internal const val ENTRY_LOCK_CODE = "lock_code"
    internal const val ENTRY_IS_SYSTEM = "is_system"
    internal const val ENTRY_ICON = "icon"

    /**
     * Starts a LockScreenActivity instance
     */
    @JvmStatic
    fun start(
      context: Context,
      entry: PadLockEntryModel,
      realName: String,
      icon: Int
    ) {
      val notPadLock = (entry.packageName() != context.applicationContext.packageName)
      val intent = Intent(context.applicationContext, LockScreenActivity::class.java).apply {
        putExtra(LockScreenActivity.ENTRY_PACKAGE_NAME, entry.packageName())
        putExtra(LockScreenActivity.ENTRY_ACTIVITY_NAME, entry.activityName())
        putExtra(LockScreenActivity.ENTRY_LOCK_CODE, entry.lockCode())
        putExtra(LockScreenActivity.ENTRY_IS_SYSTEM, entry.systemApplication())
        putExtra(LockScreenActivity.ENTRY_REAL_NAME, realName)
        putExtra(LockScreenActivity.ENTRY_ICON, icon)

        // Launch into new task
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        // If we are not locking PadLock, do a little differently
        if (notPadLock) {
          addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
          addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
      }

      if (entry.whitelist()) {
        throw RuntimeException("Cannot launch LockScreen for whitelisted applications")
      }

      Timber.d(
          "Start lock activity for entry: ${entry.packageName()} ${entry.activityName()} (real $realName)"
      )
      context.applicationContext.startActivity(intent)
    }
  }
}
