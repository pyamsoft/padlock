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
import com.pyamsoft.padlock.model.service.ServicePauseState.PAUSED
import com.pyamsoft.padlock.model.service.ServicePauseState.TEMP_PAUSED
import com.pyamsoft.padlock.service.ServiceManager.Commands
import com.pyamsoft.padlock.service.ServiceManager.Commands.PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.START
import com.pyamsoft.padlock.service.ServiceManager.Commands.TEMP_PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.USER_PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.USER_TEMP_PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Companion.FORCE_REFRESH_ON_OPEN
import com.pyamsoft.padlock.service.device.UsagePermissionChecker
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal class ServiceManagerImpl @Inject internal constructor(
  context: Context,
  private val enforcer: Enforcer,
  private val activityClass: Class<out Activity>,
  private val serviceClass: Class<out Service>,
  private val masterPinPreferences: MasterPinPreferences,
  private val servicePreferences: ServicePreferences
) : ServiceManager {

  private val appContext = context.applicationContext

  override fun startService(restart: Boolean) {
    Completable.fromAction {
      enforcer.assertNotOnMainThread()
      if (!UsagePermissionChecker.hasPermission(appContext)) {
        Timber.w("Cannot start service, missing usage permission")
        return@fromAction
      }

      if (masterPinPreferences.getMasterPassword() == null) {
        Timber.w("Cannot start service, missing master password")
        return@fromAction
      }

      val pausedState = servicePreferences.getPaused()
      if (pausedState == PAUSED && !restart) {
        Timber.d("Starting service but in paused state")
        // We don't use startForeground because starting in paused mode will not call foreground service
        // NOTE: Can crash if service is not already running from the Foreground.
        //       most noticed in BootReceiver if restart is false
        appContext.startService(service(PAUSE))
      } else if (pausedState == TEMP_PAUSED && !restart) {
        Timber.d("Starting service but in temp_paused state")
        // We don't use startForeground because starting in paused mode will not call foreground service
        // NOTE: Can crash if service is not already running from the Foreground.
        //       most noticed in BootReceiver if restart is false
        appContext.startService(service(TEMP_PAUSE))
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
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(object : CompletableObserver {

          override fun onComplete() {
            Timber.d("Service started")
          }

          override fun onSubscribe(d: Disposable) {
            Timber.d("Starting service...")
          }

          override fun onError(e: Throwable) {
            Timber.e(e, "Failed to start service")
          }

        })
  }

  @CheckResult
  private fun mainActivity(forceRefreshOnOpen: Boolean): Intent {
    return Intent(appContext, activityClass).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
      putExtra(FORCE_REFRESH_ON_OPEN, forceRefreshOnOpen)
    }
  }

  override fun mainActivityIntent(forceRefreshOnOpen: Boolean): PendingIntent {
    val intent = mainActivity(forceRefreshOnOpen)
    return PendingIntent.getActivity(
        appContext, REQUEST_CODE_MAIN_ACTIVITY, intent, PendingIntent.FLAG_ONE_SHOT
    )
  }

  @CheckResult
  private fun service(command: Commands): Intent {
    return Intent(appContext, serviceClass).apply {
      putExtra(ServiceManager.SERVICE_COMMAND, command.name)
    }
  }

  override fun startIntent(): PendingIntent {
    val intent = service(START)
    return PendingIntent.getService(
        appContext, REQUEST_CODE_SERVICE_START, intent, PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  override fun userPauseIntent(): PendingIntent {
    val intent = service(USER_PAUSE)
    return PendingIntent.getService(
        appContext, REQUEST_CODE_SERVICE_USER_PAUSE, intent, PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  override fun tempPauseIntent(): PendingIntent {
    val intent = service(USER_TEMP_PAUSE)
    return PendingIntent.getService(
        appContext, REQUEST_CODE_SERVICE_USER_TEMP_PAUSE, intent, PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  companion object {

    private const val REQUEST_CODE_MAIN_ACTIVITY = 1004
    private const val REQUEST_CODE_SERVICE_START = 1005
    private const val REQUEST_CODE_SERVICE_USER_PAUSE = 1006
    private const val REQUEST_CODE_SERVICE_USER_TEMP_PAUSE = 1007
  }

}
