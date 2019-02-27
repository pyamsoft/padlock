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

package com.pyamsoft.padlock.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.lock.screen.LockScreenViewModel
import com.pyamsoft.padlock.lock.screen.PinScreenInputViewModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.ActivityBase
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class LockScreenActivity : ActivityBase(), CloseOldPresenter.Callback {

  @field:Inject internal lateinit var lockScreen: LockScreenView
  @field:Inject internal lateinit var viewModel: LockScreenViewModel
  @field:Inject internal lateinit var inputViewModel: PinScreenInputViewModel
  @field:Inject internal lateinit var theming: Theming

  @field:Inject internal lateinit var closeOldPresenter: CloseOldPresenter

  private lateinit var lockScreenComponent: LockScreenComponent
  private lateinit var lockedActivityName: String
  private lateinit var lockedPackageName: String
  private lateinit var lockedRealName: String
  private var lockedSystem: Boolean = false
  private var lockedCode: String? = null
  private var lockedIcon: Int = 0

  private var alreadyUnlockedDisposable by singleDisposable()
  private var lockScreenTypeDisposable by singleDisposable()
  private var displayNameDisposable by singleDisposable()
  private var ignoreTimeDisposable by singleDisposable()

  override val fragmentContainerId: Int = R.id.lock_screen_container

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
    getValuesFromBundle()

    lockScreenComponent = Injector.obtain<PadLockComponent>(applicationContext)
        .plusLockScreenComponent()
        .activity(this)
        .savedInstanceState(savedInstanceState)
        .packageName(lockedPackageName)
        .activityName(lockedActivityName)
        .realName(lockedRealName)
        .lockedIcon(lockedIcon)
        .system(lockedSystem)
        .build()

    lockScreenComponent.inject(this)

    if (theming.isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Lock)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Lock)
    }
    super.onCreate(savedInstanceState)
    lockScreen.create()

    lockScreen.onToolbarNavigationClicked { onBackPressed() }

    lockScreen.onMenuItemClicked {
      when (it.itemId) {
        R.id.menu_exclude -> it.isChecked = !it.isChecked
        R.id.menu_lockscreen_info -> LockedStatDialog.newInstance().show(this, "info_dialog")
        else -> it.isChecked = true
      }
    }

    ignoreTimeDisposable =
      viewModel.createWithDefaultIgnoreTime { lockScreen.initIgnoreTimeSelection(it) }

    displayNameDisposable = viewModel.loadDisplayNameFromPackage { lockScreen.setToolbarTitle(it) }

    lockScreenTypeDisposable = inputViewModel.resolveLockScreenType(
        onTypeText = { onTypeText() },
        onTypePattern = { onTypePattern() }
    )

    closeOldPresenter.bind(this, this)
  }

  override fun onCloseOld() {
    Timber.w("Close event received for this old lock screen: $this")
    finish()
  }

  override fun onResume() {
    super.onResume()
    checkIfAlreadyUnlocked()
  }

  private fun checkIfAlreadyUnlocked() {
    alreadyUnlockedDisposable = viewModel.checkIfAlreadyUnlocked { onAlreadyUnlocked() }
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

  private fun onAlreadyUnlocked() {
    Timber.d("$lockedPackageName $lockedActivityName is already unlocked")
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
          .add(fragmentContainerId, pushFragment, tag)
          .commit(this)
    }
  }

  private fun onTypePattern() {
    pushFragment(
        LockScreenPatternFragment.newInstance(lockedCode, lockedSystem),
        LockScreenPatternFragment.TAG
    )
  }

  private fun onTypeText() {
    pushFragment(
        LockScreenTextFragment.newInstance(lockedCode, lockedSystem),
        LockScreenTextFragment.TAG
    )
  }

  override fun onPause() {
    super.onPause()
    if (isFinishing || isChangingConfigurations) {
      lockScreen.closeToolbar()
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
    displayNameDisposable.tryDispose()
    ignoreTimeDisposable.tryDispose()
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    outState?.let { lockScreen.saveState(it) }
    super.onSaveInstanceState(outState)
  }

  companion object {

    private val home = Intent(Intent.ACTION_MAIN)

    internal const val ENTRY_PACKAGE_NAME = "entry_packagename"
    internal const val ENTRY_ACTIVITY_NAME = "entry_activityname"
    internal const val ENTRY_REAL_NAME = "real_name"
    internal const val ENTRY_LOCK_CODE = "lock_code"
    internal const val ENTRY_IS_SYSTEM = "is_system"
    internal const val ENTRY_ICON = "icon"

    init {
      home.addCategory(Intent.CATEGORY_HOME)
      home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

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
