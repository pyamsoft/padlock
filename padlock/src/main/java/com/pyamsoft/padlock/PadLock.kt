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

package com.pyamsoft.padlock

import android.app.Application
import android.support.annotation.CheckResult
import android.support.v4.app.Fragment
import com.pyamsoft.padlock.base.PadLockModule
import com.pyamsoft.padlock.lock.LockHelper
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.lock.SHA256LockHelper
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.service.RecheckService
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.about.Licenses
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

class PadLock : Application(), ComponentProvider {

  private var refWatcher: RefWatcher? = null
  private var component: PadLockComponent? = null

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }

    PYDroid.initialize(this, BuildConfig.DEBUG)
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite")
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight")
    Licenses.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2")
    Licenses.create("Firebase", "https://firebase.google.com", "licenses/firebase")
    Licenses.create("PatternLockView", "https://github.com/aritraroy/PatternLockView",
        "licenses/patternlock")

    LockHelper.set(SHA256LockHelper.newInstance())
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
    Injector.set(dagger)
    component = dagger

    if (BuildConfig.DEBUG) {
      refWatcher = LeakCanary.install(this)
    } else {
      refWatcher = RefWatcher.DISABLED
    }
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

  override fun getComponent(): PadLockComponent {
    val obj = component
    if (obj == null) {
      throw IllegalStateException("PadLockComponent must be initialized before use")
    } else {
      return obj
    }
  }

  companion object {

    @JvmStatic
    @CheckResult fun getRefWatcher(fragment: CanaryFragment): RefWatcher {
      return getRefWatcherInternal(fragment)
    }

    @JvmStatic
    @CheckResult fun getRefWatcher(fragment: CanaryDialog): RefWatcher {
      return getRefWatcherInternal(fragment)
    }

    @JvmStatic
    @CheckResult fun getRefWatcher(fragment: ActionBarSettingsPreferenceFragment): RefWatcher {
      return getRefWatcherInternal(fragment)
    }

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
