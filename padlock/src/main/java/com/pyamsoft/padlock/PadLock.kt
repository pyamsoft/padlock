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
import com.pyamsoft.padlock.base.PadLockProvider
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.service.RecheckService
import com.pyamsoft.padlock.settings.SettingsFragment
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.PYDroidModule
import com.pyamsoft.pydroid.about.Licenses
import com.pyamsoft.pydroid.loader.LoaderModule
import com.pyamsoft.pydroid.ui.PYDroid
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

class PadLock : Application() {

  private lateinit var refWatcher: RefWatcher
  private var component: PadLockComponent? = null
  private lateinit var pydroidModule: PYDroidModule
  private lateinit var loaderModule: LoaderModule

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

    pydroidModule = PYDroidModule(this, BuildConfig.DEBUG)
    loaderModule = LoaderModule(this)
    PYDroid.init(pydroidModule, loaderModule)
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite")
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight")
    Licenses.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2")
    Licenses.create("FastAdapter", "https://github.com/mikepenz/fastadapter",
        "licenses/fastadapter")
    Licenses.create("Firebase", "https://firebase.google.com", "licenses/firebase")
    Licenses.create("PatternLockView", "https://github.com/aritraroy/PatternLockView",
        "licenses/patternlock")

    val dagger = Injector.obtain<PadLockComponent>(applicationContext)
    val receiver = dagger.provideApplicationInstallReceiver()
    val preferences = dagger.provideInstallListenerPreferences()
    if (preferences.isInstallListenerEnabled()) {
      receiver.register()
    } else {
      receiver.unregister()
    }
  }

  private fun buildDagger(): PadLockComponent {
    val provider = PadLockProvider(pydroidModule, loaderModule, MainActivity::class.java,
        LockScreenActivity::class.java,
        RecheckService::class.java)
    return DaggerPadLockComponent.builder().padLockProvider(provider).build()
  }

  override fun getSystemService(name: String?): Any {
    return if (Injector.name == name) {
      val graph: PadLockComponent
      val dagger = component
      if (dagger == null) {
        graph = buildDagger()
        component = graph
      } else {
        graph = dagger
      }

      // Return
      graph
    } else {
      // Return
      super.getSystemService(name)
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun getRefWatcher(fragment: CanaryFragment): RefWatcher = getRefWatcherInternal(fragment)

    @CheckResult
    @JvmStatic
    fun getRefWatcher(fragment: CanaryDialog): RefWatcher = getRefWatcherInternal(fragment)

    @JvmStatic
    @CheckResult
    fun getRefWatcher(fragment: SettingsFragment): RefWatcher = getRefWatcherInternal(fragment)

    @JvmStatic
    @CheckResult
    private fun getRefWatcherInternal(fragment: Fragment): RefWatcher {
      val application = fragment.activity!!.application
      if (application is PadLock) {
        return application.refWatcher
      } else {
        throw IllegalStateException("Application is not PadLock")
      }
    }
  }
}
