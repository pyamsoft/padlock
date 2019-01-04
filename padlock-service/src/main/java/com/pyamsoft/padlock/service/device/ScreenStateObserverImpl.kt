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

package com.pyamsoft.padlock.service.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.api.service.ScreenStateObserver
import javax.inject.Inject
import javax.inject.Singleton

internal class ScreenStateObserverImpl @Inject internal constructor(
  private val context: Context
) : BroadcastReceiver(), ScreenStateObserver {

  private val displayManager = requireNotNull(context.getSystemService<DisplayManager>())
  private var callback: ((Boolean) -> Unit)? = null

  @CheckResult
  private fun isDisplayOff(): Boolean {
    return displayManager.getDisplay(Display.DEFAULT_DISPLAY).state == Display.STATE_OFF
  }

  override fun onReceive(
    context: Context?,
    intent: Intent?
  ) {
    intent?.also {
      val action = it.action
      when (action) {
        Intent.ACTION_SCREEN_OFF -> {
          if (isDisplayOff()) {
            callback?.invoke(false)
          }
        }
        Intent.ACTION_SCREEN_ON -> {
          if (!isDisplayOff()) {
            callback?.invoke(true)
          }
        }
      }
    }
  }

  override fun register(func: (Boolean) -> Unit) {
    if (callback == null) {
      callback = func
      context.registerReceiver(this, SCREEN_EVENT_FILTER)
    }
  }

  override fun unregister() {
    if (callback != null) {
      callback = null
      context.unregisterReceiver(this)
    }
  }

  companion object {

    private val SCREEN_EVENT_FILTER = IntentFilter()

    init {
      SCREEN_EVENT_FILTER.addAction(Intent.ACTION_SCREEN_OFF)
      SCREEN_EVENT_FILTER.addAction(Intent.ACTION_SCREEN_ON)
    }
  }
}
