package com.pyamsoft.padlock.service

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.preferences.MasterPinPreferences
import com.pyamsoft.padlock.api.preferences.ServicePreferences
import com.pyamsoft.padlock.service.ServiceManager.Commands.PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.START
import com.pyamsoft.padlock.service.ServiceManager.Commands.TEMP_PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.USER_PAUSE
import com.pyamsoft.padlock.service.device.UsagePermissionChecker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ServiceManager @Inject internal constructor(
  context: Context,
  @Named("main_activity") private val activityClass: Class<out Activity>,
  @Named("service") private val serviceClass: Class<out Service>,
  private val masterPinPreferences: MasterPinPreferences,
  private val servicePreferences: ServicePreferences
) {

  private val appContext = context.applicationContext

  fun startService(restart: Boolean) {
    if (!UsagePermissionChecker.hasPermission(appContext)) {
      Timber.w("Cannot start service, missing usage permission")
      return
    }

    if (masterPinPreferences.getMasterPassword() == null) {
      Timber.w("Cannot start service, missing master password")
      return
    }

    if (servicePreferences.isPaused() && !restart) {
      Timber.d("Starting service but in paused state")
      // We don't use startForeground because starting in paused mode will not call foreground service
      // NOTE: Can crash if service is not already running from the Foreground.
      //       most noticed in BootReceiver if restart is false
      appContext.startService(service(PAUSE))
    } else {
      if (restart) {
        Timber.d("Restarting service")
      } else {
        Timber.d("Starting service")
      }
      val intent = service(START)
      if (VERSION.SDK_INT >= VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      } else {
        appContext.startService(intent)
      }
    }
  }

  @CheckResult
  private fun mainActivity(forceRefreshOnOpen: Boolean): Intent {
    return Intent(appContext, activityClass).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
      putExtra(FORCE_REFRESH_ON_OPEN, forceRefreshOnOpen)
    }
  }

  @CheckResult
  fun mainActivityIntent(forceRefreshOnOpen: Boolean): PendingIntent {
    val intent = mainActivity(forceRefreshOnOpen)
    return PendingIntent.getActivity(
        appContext, REQUEST_CODE_MAIN_ACTIVITY, intent, PendingIntent.FLAG_ONE_SHOT
    )
  }

  @CheckResult
  private fun service(command: Commands): Intent {
    return Intent(appContext, serviceClass).apply {
      putExtra(SERVICE_COMMAND, command.name)
    }
  }

  @CheckResult
  fun startIntent(): PendingIntent {
    val intent = service(START)
    return PendingIntent.getService(
        appContext, REQUEST_CODE_SERVICE_START, intent, PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  @CheckResult
  fun userPauseIntent(): PendingIntent {
    val intent = service(USER_PAUSE)
    return PendingIntent.getService(
        appContext, REQUEST_CODE_SERVICE_USER_PAUSE, intent, PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  @CheckResult
  fun tempPauseIntent(): PendingIntent {
    val intent = service(TEMP_PAUSE)
    return PendingIntent.getService(
        appContext, REQUEST_CODE_SERVICE_TEMP_PAUSE, intent, PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  companion object {

    const val SERVICE_COMMAND = "SERVICE_COMMAND"
    const val FORCE_REFRESH_ON_OPEN = "FORCE_REFRESH_ON_OPEN"

    private const val REQUEST_CODE_MAIN_ACTIVITY = 1004
    private const val REQUEST_CODE_SERVICE_START = 1005
    private const val REQUEST_CODE_SERVICE_USER_PAUSE = 1006
    private const val REQUEST_CODE_SERVICE_TEMP_PAUSE = 1007
  }

  enum class Commands {
    START,
    PAUSE,
    USER_PAUSE,
    TEMP_PAUSE
  }

}
