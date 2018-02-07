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

package com.pyamsoft.padlock

import android.app.Application
import android.app.Service
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.PadLockProvider
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.service.RecheckService
import com.pyamsoft.padlock.settings.PadLockPreferenceFragment
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.PYDroidModule
import com.pyamsoft.pydroid.PYDroidModuleImpl
import com.pyamsoft.pydroid.base.about.Licenses
import com.pyamsoft.pydroid.loader.LoaderModule
import com.pyamsoft.pydroid.loader.LoaderModuleImpl
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

    pydroidModule = PYDroidModuleImpl(this, BuildConfig.DEBUG)
    loaderModule = LoaderModuleImpl(pydroidModule)
    PYDroid.init(pydroidModule, loaderModule)
    Licenses.create("SQLBrite", "https://github.com/square/sqlbrite", "licenses/sqlbrite")
    Licenses.create("SQLDelight", "https://github.com/square/sqldelight", "licenses/sqldelight")
    Licenses.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2")
    Licenses.create(
        "FastAdapter", "https://github.com/mikepenz/fastadapter",
        "licenses/fastadapter"
    )
    Licenses.create("Firebase", "https://firebase.google.com", "licenses/firebase")
    Licenses.create(
        "PatternLockView", "https://github.com/aritraroy/PatternLockView",
        "licenses/patternlock"
    )

    val dagger = Injector.obtain<PadLockComponent>(applicationContext)
    val receiver = dagger.provideApplicationInstallReceiver()
    val preferences = dagger.provideInstallListenerPreferences()
    if (preferences.isInstallListenerEnabled()) {
      receiver.register()
    } else {
      receiver.unregister()
    }

    PadLockService.start(this)
  }

  private fun buildDagger(): PadLockComponent {
    val provider = PadLockProvider(
        pydroidModule, loaderModule, MainActivity::class.java,
        RecheckService::class.java
    )
    return DaggerPadLockComponent.builder()
        .padLockProvider(provider)
        .build()
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
    fun getRefWatcher(fragment: CanaryFragment): RefWatcher = getRefWatcherInternal(
        fragment.activity!!.application
    )

    @CheckResult
    @JvmStatic
    fun getRefWatcher(fragment: CanaryDialog): RefWatcher = getRefWatcherInternal(
        fragment.activity!!.application
    )

    @JvmStatic
    @CheckResult
    fun getRefWatcher(
        preferenceFragment: PadLockPreferenceFragment
    ): RefWatcher = getRefWatcherInternal(
        preferenceFragment.activity!!.application
    )

    @JvmStatic
    @CheckResult
    fun getRefWatcher(service: Service): RefWatcher = getRefWatcherInternal(service.application)

    @JvmStatic
    @CheckResult
    private fun getRefWatcherInternal(application: Application): RefWatcher {
      if (application is PadLock) {
        return application.refWatcher
      } else {
        throw IllegalStateException("Application is not PadLock")
      }
    }
  }
}
