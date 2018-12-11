package com.pyamsoft.padlock.service

import android.app.PendingIntent
import androidx.annotation.CheckResult

interface ServiceManager {

  fun startService(restart: Boolean)

  @CheckResult
  fun mainActivityIntent(forceRefreshOnOpen: Boolean): PendingIntent

  @CheckResult
  fun startIntent(): PendingIntent

  @CheckResult
  fun userPauseIntent(): PendingIntent

  @CheckResult
  fun tempPauseIntent(): PendingIntent

  companion object {

    const val SERVICE_COMMAND = "SERVICE_COMMAND"
    const val FORCE_REFRESH_ON_OPEN = "FORCE_REFRESH_ON_OPEN"

  }

  enum class Commands {
    START,
    PAUSE,
    TEMP_PAUSE,
    USER_PAUSE,
    USER_TEMP_PAUSE
  }

}
