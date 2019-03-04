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
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.pin.ConfirmPinView
import com.pyamsoft.padlock.service.ForegroundEventPresenter
import com.pyamsoft.pydroid.ui.app.ActivityBase
import com.pyamsoft.pydroid.ui.theme.ThemeInjector
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class LockScreenActivity : ActivityBase(),
    LockScreenPresenter.Callback {

  @field:Inject internal lateinit var toolbar: LockToolbarView
  @field:Inject internal lateinit var iconView: LockImageView
  @field:Inject internal lateinit var pinScreen: ConfirmPinView

  @field:Inject internal lateinit var presenter: LockScreenPresenter
  @field:Inject internal lateinit var foregroundPresenter: ForegroundEventPresenter

  override val fragmentContainerId: Int = 0

  private lateinit var layoutRoot: ConstraintLayout

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    if (ThemeInjector.obtain(applicationContext).isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Lock)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Lock)
    }
    overridePendingTransition(0, 0)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_constraint)
    layoutRoot = findViewById(R.id.layout_constraint)

    Injector.obtain<PadLockComponent>(applicationContext)
        .plusLockComponent()
        .toolbarActivityProvider(this)
        .packageName(getLockedPackageName())
        .activityName(getLockedActivityName())
        .realName(getLockedRealName())
        .appIcon(getLockedIcon())
        .build()
        .inject(this)

    createComponents(savedInstanceState)
    layoutComponents(layoutRoot)

    presenter.bind(this, this)

    if (savedInstanceState == null) {
      Timber.d("No savedInstanceState, load default ignore time")
      presenter.loadDefaultIgnoreTime()
    }
  }

  private fun createComponents(savedInstanceState: Bundle?) {
    toolbar.inflate(savedInstanceState)
    iconView.inflate(savedInstanceState)
    pinScreen.inflate(savedInstanceState)
  }

  private fun layoutComponents(layoutRoot: ConstraintLayout) {
    ConstraintSet().apply {
      clone(layoutRoot)

      toolbar.also {
        connect(it.id(), ConstraintSet.TOP, layoutRoot.id, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, layoutRoot.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, layoutRoot.id, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      iconView.also {
        connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, layoutRoot.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, layoutRoot.id, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      pinScreen.also {
        connect(it.id(), ConstraintSet.TOP, iconView.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, layoutRoot.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, layoutRoot.id, ConstraintSet.END)
        connect(it.id(), ConstraintSet.BOTTOM, layoutRoot.id, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      applyTo(layoutRoot)
    }

  }

  override fun showLockedStats() {
    LockedStatDialog.newInstance()
        .show(this, "info_dialog")
  }

  override fun onCloseOld() {
    Timber.w("Close event received for this old lock screen: $this")
    finish()
  }

  override fun onDisplayNameLoaded(name: String) {
    toolbar.setName(name)
  }

  override fun onDefaultIgnoreTimeLoaded(time: Long) {
    toolbar.initIgnoreTime(time)
  }

  override fun onResume() {
    super.onResume()
    presenter.checkUnlocked()
  }

  override fun onShowLockHint(hint: String) {
    pinScreen.showHint(hint)
  }

  override fun onSubmitUnlockAttempt(attempt: String) {
    val excluded = toolbar.isExcludeChecked()
    val ignoreTime = toolbar.getSelectedIgnoreTime()
    presenter.submit(getLockedCode(), attempt, getLockedIsSystem(), excluded, ignoreTime)
  }

  @CheckResult
  private fun getLockedPackageName(): String {
    return requireNotNull(intent.extras).getString(ENTRY_PACKAGE_NAME, "")
        .also { require(it.isNotBlank()) }
  }

  @CheckResult
  private fun getLockedActivityName(): String {
    return requireNotNull(intent.extras).getString(ENTRY_ACTIVITY_NAME, "")
        .also { require(it.isNotBlank()) }
  }

  @CheckResult
  private fun getLockedRealName(): String {
    return requireNotNull(intent.extras).getString(ENTRY_REAL_NAME, "")
        .also { require(it.isNotBlank()) }
  }

  @CheckResult
  private fun getLockedCode(): String? {
    return requireNotNull(intent.extras).getString(ENTRY_LOCK_CODE)
  }

  @CheckResult
  private fun getLockedIcon(): Int {
    return requireNotNull(intent.extras).getInt(ENTRY_ICON, 0)
  }

  @CheckResult
  private fun getLockedIsSystem(): Boolean {
    return requireNotNull(intent.extras).getBoolean(ENTRY_IS_SYSTEM, false)
  }

  override fun onAlreadyUnlocked() {
    Timber.d("${getLockedPackageName()} ${getLockedActivityName()} unlocked, close lock screen")
    finish()
  }

  override fun onSubmitBegin() {
    Timber.d("Submit begin")
    pinScreen.disable()
  }

  override fun onSubmitUnlocked() {
    Timber.d(
        "Unlocked! ${getLockedPackageName()} ${getLockedActivityName()} ${getLockedRealName()}"
    )
    pinScreen.clearDisplay()
    finish()
  }

  override fun onSubmitLocked() {
    Timber.e(
        "Temp Locked ${getLockedPackageName()} ${getLockedActivityName()} ${getLockedRealName()}"
    )
    pinScreen.clearDisplay()
    pinScreen.enable()
    pinScreen.showErrorMessage("Error: This App is temporarily locked.")
  }

  override fun onSubmitFailed() {
    Timber.w(
        "Failed unlock ${getLockedPackageName()} ${getLockedActivityName()} ${getLockedRealName()}"
    )
    pinScreen.clearDisplay()
    pinScreen.enable()
    pinScreen.showErrorMessage("Error: Invalid PIN")
  }

  override fun onPause() {
    super.onPause()
    if (isFinishing || isChangingConfigurations) {
      toolbar.close()
    }

    // Clear the current foreground
    foregroundPresenter.foreground(getLockedPackageName(), getLockedRealName())
  }

  override fun onBackPressed() {
    applicationContext.startActivity(home)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    pinScreen.teardown()
    iconView.teardown()
    toolbar.teardown()
    overridePendingTransition(0, 0)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    pinScreen.saveState(outState)
    iconView.saveState(outState)
    toolbar.saveState(outState)
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
      val appContext = context.applicationContext
      val notPadLock = (entry.packageName() != appContext.packageName)
      val intent = Intent(appContext, LockScreenActivity::class.java).apply {
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

      Timber.d("Lock: ${entry.packageName()} ${entry.activityName()} (real $realName)")
      appContext.startActivity(intent)
    }
  }
}
