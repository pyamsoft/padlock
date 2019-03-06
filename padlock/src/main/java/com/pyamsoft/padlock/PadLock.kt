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

package com.pyamsoft.padlock

import android.app.Application
import android.app.Service
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.api.preferences.InstallListenerPreferences
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.ui.theme.ThemeInjector
import com.pyamsoft.pydroid.ui.theme.Theming
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import javax.inject.Inject

class PadLock : Application(), PYDroid.Instance {

  private var pyDroid: PYDroid? = null
  private lateinit var component: PadLockComponent
  private lateinit var theming: Theming
  private lateinit var refWatcher: RefWatcher

  @field:Inject internal lateinit var installListenerPreferences: InstallListenerPreferences
  @field:Inject internal lateinit var receiver: ApplicationInstallReceiver

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }

    Theming.IS_DEFAULT_DARK_THEME = false
    PYDroid.init(
        this,
        this,
        getString(R.string.app_name),
        "https://github.com/pyamsoft/padlock/issues",
        BuildConfig.VERSION_CODE,
        BuildConfig.DEBUG
    )

    installRefWatcher()
    addLibraries()
    createDagger()
    listenForNewAppInstalls()
  }

  private fun listenForNewAppInstalls() {
    if (installListenerPreferences.isInstallListenerEnabled()) {
      receiver.register()
    } else {
      receiver.unregister()
    }
  }

  private fun createDagger() {
    val dagger = Injector.obtain<PadLockComponent>(this)
    dagger.inject(this)
  }

  private fun installRefWatcher() {
    if (BuildConfig.DEBUG) {
      refWatcher = LeakCanary.install(this)
    } else {
      refWatcher = RefWatcher.DISABLED
    }
  }

  private fun addLibraries() {
    OssLibraries.add(
        "Room",
        "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/room/",
        "The AndroidX Jetpack Room library. Fluent SQLite database access."
    )
    OssLibraries.add(
        "Dagger",
        "https://github.com/google/dagger",
        "A fast dependency injector for Android and Java."
    )
    OssLibraries.add(
        "FastAdapter",
        "https://github.com/mikepenz/fastadapter",
        "The bullet proof, fast and easy to use adapter library, which minimizes developing time to a fraction..."
    )
    OssLibraries.add(
        "PatternLockView",
        "https://github.com/aritraroy/PatternLockView",
        "An easy-to-use, customizable and Material Design ready Pattern Lock view for Android."
    )
  }

  override fun getPydroid(): PYDroid? = pyDroid

  override fun setPydroid(instance: PYDroid) {
    pyDroid = instance.also {
      theming = it.modules()
          .theming()
      component = DaggerPadLockComponent.builder()
          .application(this)
          .enforcer(it.modules().enforcer())
          .imageLoader(it.modules().loaderModule().provideImageLoader())
          .theming(theming)
          .moshi(it.modules().versionCheckModule().moshi)
          .build()
    }
  }

  override fun getSystemService(name: String): Any {
    when (name) {
      Injector.name -> return component
      ThemeInjector.name -> return theming
      else -> return super.getSystemService(name)
    }
  }

  companion object {

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
