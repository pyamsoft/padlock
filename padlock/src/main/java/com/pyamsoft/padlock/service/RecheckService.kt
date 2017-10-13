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

import android.app.IntentService
import android.content.Intent
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.lock.Recheck
import timber.log.Timber
import javax.inject.Inject

class RecheckService : IntentService(RecheckService::class.java.name) {

  @field:Inject internal lateinit var recheckBus: RecheckPublisher

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext).inject(this)
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
