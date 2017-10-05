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

package com.pyamsoft.padlock

import android.app.Application
import android.support.annotation.CheckResult
import android.support.v4.app.Fragment
import com.pyamsoft.padlock.base.PadLockModule
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.service.RecheckService
import com.pyamsoft.padlock.settings.SettingsFragment
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.about.Licenses
import com.pyamsoft.pydroid.ui.PYDroid
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

class PadLock : Application() {

  private var refWatcher: RefWatcher? = null
  private var component: PadLockComponent? = null

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }

    refWatcher = if (BuildConfig.DEBUG) {
      LeakCanary.install(this)
    } else {
      RefWatcher.DISABLED
    }

    PYDroid.initialize(this, BuildConfig.DEBUG)
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite")
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight")
    Licenses.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2")
    Licenses.create("FastAdapter", "https://github.com/mikepenz/fastadapter",
        "licenses/fastadapter")
    Licenses.create("Firebase", "https://firebase.google.com", "licenses/firebase")
    Licenses.create("PatternLockView", "https://github.com/aritraroy/PatternLockView",
        "licenses/patternlock")

    val padLockModule = PadLockModule(applicationContext, MainActivity::class.java,
        LockScreenActivity::class.java,
        RecheckService::class.java)
    val dagger = DaggerPadLockComponent.builder().padLockModule(padLockModule).build()

    val receiver = dagger.provideApplicationInstallReceiver()
    val preferences = dagger.provideInstallListenerPreferences()
    if (preferences.isInstallListenerEnabled()) {
      receiver.register()
    } else {
      receiver.unregister()
    }
    component = dagger
  }

  private val watcher: RefWatcher
    @CheckResult get() {
      val obj = refWatcher
      if (obj == null) {
        throw IllegalStateException("RefWatcher is NULL")
      } else {
        return obj
      }
    }

  override fun getSystemService(name: String?): Any {
    return if (Injector.name == name) {
      // Return
      component ?: throw IllegalStateException("PadLock component is NULL")
    } else {

      // Return
      super.getSystemService(name)
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun getRefWatcher(fragment: CanaryFragment): RefWatcher = getRefWatcherInternal(fragment)

    @JvmStatic
    @CheckResult
    fun getRefWatcher(fragment: CanaryDialog): RefWatcher = getRefWatcherInternal(fragment)

    @JvmStatic
    @CheckResult
    fun getRefWatcher(fragment: SettingsFragment): RefWatcher = getRefWatcherInternal(fragment)

    @JvmStatic
    @CheckResult
    private fun getRefWatcherInternal(fragment: Fragment): RefWatcher {
      val application = fragment.activity.application
      if (application is PadLock) {
        return application.watcher
      } else {
        throw IllegalStateException("Application is not PadLock")
      }
    }
  }
}
