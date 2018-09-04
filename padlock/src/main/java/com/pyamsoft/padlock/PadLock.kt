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
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.service.RecheckService
import com.pyamsoft.pydroid.bootstrap.about.AboutLibraries
import com.pyamsoft.pydroid.ui.PYDroid
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

class PadLock : Application(), PYDroid.Instance {

  private var pyDroid: PYDroid? = null
  private lateinit var component: PadLockComponent
  private lateinit var refWatcher: RefWatcher

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }

    if (BuildConfig.DEBUG) {
      refWatcher = LeakCanary.install(this)
    } else {
      refWatcher = RefWatcher.DISABLED
    }

    AboutLibraries.create("Room", "https://source.android.com", "licenses/room")
    AboutLibraries.create("Dagger", "https://github.com/google/dagger", "licenses/dagger2")
    AboutLibraries.create(
        "FastAdapter", "https://github.com/mikepenz/fastadapter", "licenses/fastadapter"
    )
    AboutLibraries.create(
        "PatternLockView", "https://github.com/aritraroy/PatternLockView", "licenses/patternlock"
    )
    PYDroid.init(this, this, BuildConfig.DEBUG)

    val dagger = Injector.obtain<PadLockComponent>(this)
    val receiver = dagger.provideApplicationInstallReceiver()
    val preferences = dagger.provideInstallListenerPreferences()
    if (preferences.isInstallListenerEnabled()) {
      receiver.register()
    } else {
      receiver.unregister()
    }

    PadLockService.start(this)
  }

  override fun getPydroid(): PYDroid? = pyDroid

  override fun setPydroid(instance: PYDroid) {
    pyDroid = instance.also {
      val provider = PadLockProvider(
          this,
          it.modules().loaderModule().provideImageLoader(),
          it.enforcer(),
          MainActivity::class.java,
          RecheckService::class.java
      )
      component = DaggerPadLockComponent.builder()
          .padLockProvider(provider)
          .build()
    }
  }

  override fun getSystemService(name: String): Any {
    if (Injector.name == name) {
      return component
    } else {
      return super.getSystemService(name)
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
