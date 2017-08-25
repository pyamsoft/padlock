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

import android.app.IntentService
import android.content.Intent
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.lock.Recheck
import timber.log.Timber
import javax.inject.Inject

class RecheckService : IntentService(RecheckService::class.java.name) {

  @field:Inject internal lateinit var recheckBus: RecheckPublisher

  override fun onCreate() {
    super.onCreate()
    Injector.with(this) {
      it.inject(this)
    }
  }

  override fun onHandleIntent(intent: Intent?) {
    if (intent == null) {
      Timber.e("Intent is NULL")
      return
    }

    if (!intent.hasExtra(Recheck.EXTRA_PACKAGE_NAME)) {
      Timber.e("No package name passed")
      return
    }

    if (!intent.hasExtra(Recheck.EXTRA_CLASS_NAME)) {
      Timber.e("No class name passed")
      return
    }

    val packageName = intent.getStringExtra(Recheck.EXTRA_PACKAGE_NAME)
    val className = intent.getStringExtra(Recheck.EXTRA_CLASS_NAME)
    if (packageName.isNotEmpty() && className.isNotEmpty()) {
      Timber.d("Recheck was requested for: %s, %s", packageName, className)
      recheckBus.publish(RecheckEvent.create(packageName, className))
    }
  }
}
