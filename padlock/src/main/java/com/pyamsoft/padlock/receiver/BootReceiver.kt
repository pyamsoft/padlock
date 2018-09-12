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

package com.pyamsoft.padlock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.service.ServiceManager
import timber.log.Timber
import javax.inject.Inject

class BootReceiver : BroadcastReceiver() {

  @field:Inject internal lateinit var serviceManager: ServiceManager

  override fun onReceive(
    context: Context?,
    intent: Intent?
  ) {
    if (intent != null) {
      if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
        if (context != null) {
          Injector.obtain<PadLockComponent>(context.applicationContext)
              .inject(this)
          Timber.d("Boot event received, start PadLockService")
          serviceManager.startService(true)
        }
      }
    }
  }
}
