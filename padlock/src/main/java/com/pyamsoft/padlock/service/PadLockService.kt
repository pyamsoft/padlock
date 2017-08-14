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

package com.pyamsoft.padlock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.support.annotation.CheckResult
import android.view.accessibility.AccessibilityEvent
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.lock.LockScreenActivity
import timber.log.Timber
import javax.inject.Inject

class PadLockService : AccessibilityService() {

  @field:Inject internal lateinit var presenter: LockServicePresenter
  protected @JvmField val startLockScreen: (PadLockEntry, String) -> Unit = { entry, realName ->
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName)
    LockScreenActivity.start(this, entry, realName)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) {
      Timber.e("AccessibilityEvent is NULL")
      return
    }

    val eventPackage = event.packageName
    val eventClass = event.className

    if (eventPackage != null && eventClass != null) {
      val pName = eventPackage.toString()
      val cName = eventClass.toString()
      if (!pName.isEmpty() && !cName.isEmpty()) {
        presenter.processAccessibilityEvent(pName, cName, RecheckStatus.NOT_FORCE,
            startLockScreen)
      }
    } else {
      Timber.e("Missing needed data")
    }
  }

  override fun onInterrupt() {
    Timber.e("onInterrupt")
  }

  override fun onUnbind(intent: Intent): Boolean {
    presenter.stop()
    isRunning = false
    return super.onUnbind(intent)
  }

  override fun onCreate() {
    super.onCreate()
    Injector.with(this) {
      it.inject(this)
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    Timber.d("onServiceConnected")
    presenter.registerOnBus(onRecheck = { packageName, className ->
      presenter.processActiveApplicationIfMatching(packageName, className, startLockScreen)
    }, onFinish = {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        disableSelf()
      }
    })
    isRunning = true
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.destroy()
  }

  companion object {

    @JvmStatic var isRunning: Boolean = false
      @CheckResult get
      private set
  }
}