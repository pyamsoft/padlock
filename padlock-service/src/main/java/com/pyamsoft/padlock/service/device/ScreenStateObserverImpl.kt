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

@Singleton
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
