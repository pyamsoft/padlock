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
import androidx.constraintlayout.widget.ConstraintLayout
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.ui.app.ActivityBase
import com.pyamsoft.pydroid.ui.theme.ThemeInjector
import timber.log.Timber
import javax.inject.Inject

class LockScreenActivity : ActivityBase(),
    LockScreenUiComponent.Callback {

  @field:Inject internal lateinit var component: LockScreenUiComponent
  @field:Inject internal lateinit var toolbarComponent: LockScreenToolbarUiComponent

  override val fragmentContainerId: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    if (ThemeInjector.obtain(applicationContext).isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Lock)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Lock)
    }
    overridePendingTransition(0, 0)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_constraint)

    val extras = requireNotNull(intent.extras)
    val lockedPackageName = extras.getString(ENTRY_PACKAGE_NAME, "")
    val lockedActivityName = extras.getString(ENTRY_ACTIVITY_NAME, "")
    val lockedRealName = extras.getString(ENTRY_REAL_NAME, "")
    val lockedCode = extras.getString(ENTRY_LOCK_CODE, null)
    val lockedIsSystem = extras.getBoolean(ENTRY_IS_SYSTEM, false)
    val lockedIcon = extras.getInt(ENTRY_ICON, 0)

    require(lockedPackageName.isNotBlank())
    require(lockedActivityName.isNotBlank())
    require(lockedRealName.isNotBlank())

    val layoutRoot = findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<PadLockComponent>(applicationContext)
        .plusLockComponent()
        .parent(layoutRoot)
        .owner(this)
        .toolbarActivityProvider(this)
        .packageName(lockedPackageName)
        .activityName(lockedActivityName)
        .realName(lockedRealName)
        .lockedCode(lockedCode)
        .lockedSystem(lockedIsSystem)
        .appIcon(lockedIcon)
        .build()
        .inject(this)

    component.bind(this, savedInstanceState, this)
    toolbarComponent.bind(this, savedInstanceState, Unit)

    toolbarComponent.layout(layoutRoot)
    component.layout(layoutRoot, toolbarComponent.id())
  }

  override fun onResume() {
    super.onResume()
    component.checkUnlocked()
  }

  override fun onClose() {
    finish()
  }

  override fun onSubmitAttempt(attempt: String) {
    val isExcluded = toolbarComponent.isExcludeChecked()
    val ignoreTime = toolbarComponent.getSelectedIgnoreTime()
    component.submit(attempt, isExcluded, ignoreTime)
  }

  override fun onPause() {
    super.onPause()
    if (isFinishing || isChangingConfigurations) {
      toolbarComponent.close()
    }
    component.clearForeground()
  }

  override fun onBackPressed() {
    applicationContext.startActivity(home)
  }

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    overridePendingTransition(0, 0)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    component.saveState(outState)
    toolbarComponent.saveState(outState)
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
