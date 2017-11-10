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

package com.pyamsoft.padlock.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.lock.LockScreenActivity
import timber.log.Timber
import javax.inject.Inject

class PadLockService : Service(), LockServicePresenter.View {

  override fun onBind(ignore: Intent?): IBinder? = null

  @field:Inject internal lateinit var presenter: LockServicePresenter

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext).inject(this)
    presenter.bind(this)
    isRunning = true
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.unbind()
    isRunning = false
  }

  override fun onFinish() {
    stopForeground(true)
    stopSelf()
  }

  private fun onForegroundEvent(eventPackage: String?, eventClass: String?) {
    if (eventPackage != null && eventClass != null) {
      val pName = eventPackage.toString()
      val cName = eventClass.toString()
      if (pName.isNotBlank() && cName.isNotBlank()) {
        presenter.processAccessibilityEvent(pName, cName, RecheckStatus.NOT_FORCE)
      }
    } else {
      Timber.e("Missing needed data")
    }
  }

  override fun onRecheck(packageName: String, className: String) {
    presenter.processActiveApplicationIfMatching(packageName, className)
  }

  override fun onStartLockScreen(entry: PadLockEntry, realName: String) {
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName)
    LockScreenActivity.start(this, entry, realName)
  }

  companion object {

    var isRunning: Boolean = false
      @CheckResult get
      private set
  }
}
