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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType.SERVICE_TEMP_PAUSE
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.service.ServicePauseState.PAUSED
import com.pyamsoft.padlock.model.service.ServicePauseState.STARTED
import com.pyamsoft.padlock.model.service.ServicePauseState.TEMP_PAUSED
import com.pyamsoft.padlock.service.ServiceManager.Commands
import com.pyamsoft.padlock.service.ServiceManager.Commands.PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.START
import com.pyamsoft.padlock.service.ServiceManager.Commands.TEMP_PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.USER_PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.USER_TEMP_PAUSE
import com.pyamsoft.padlock.uicommon.UsageAccessRequestDelegate
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.util.fakeBind
import com.pyamsoft.pydroid.util.fakeUnbind
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class PadLockService : Service(),
    LifecycleOwner,
    ServicePausePresenter.Callback,
    ServiceFinishPresenter.Callback,
    PermissionPresenter.Callback {

  private val notificationManager by lazy(NONE) {
    requireNotNull(application.getSystemService<NotificationManager>())
  }
  private val registry = LifecycleRegistry(this)

  private lateinit var notificationBuilder: NotificationCompat.Builder

  @field:Inject internal lateinit var serviceManager: ServiceManager
  @field:Inject internal lateinit var jobSchedulerCompat: JobSchedulerCompat

  @field:Inject internal lateinit var viewModel: LockServiceViewModel
  @field:Inject internal lateinit var permissionPresenter: PermissionPresenter
  @field:Inject internal lateinit var pausePresenter: ServicePausePresenter
  @field:Inject internal lateinit var finishPresenter: ServiceFinishPresenter

  private var screenStateDisposable by singleDisposable()
  private var foregroundDisposable by singleDisposable()
  private var recheckDisposable by singleDisposable()

  override fun getLifecycle(): Lifecycle {
    return registry
  }

  override fun onBind(ignore: Intent?): IBinder? {
    throw AssertionError("Service is not bound")
  }

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext)
        .inject(this)

    setupNotifications()

    screenStateDisposable = viewModel.observeScreenState(
        onScreenOn = {
          foregroundDisposable.tryDispose()
          recheckDisposable.tryDispose()
          beginWatchingForLockedApplications()
        },
        onScreenOff = {
          foregroundDisposable.tryDispose()
          recheckDisposable.tryDispose()
        }
    )

    pausePresenter.bind(this, this)
    finishPresenter.bind(this, this)
    permissionPresenter.bind(this, this)

    registry.fakeBind()
  }

  override fun onPermissionLost() {
    servicePermissionLost()
  }

  override fun onServiceFinished() {
    serviceStop()
  }

  override fun onServicePaused(autoResume: Boolean) {
    pauseService(autoResume)
  }

  private fun beginWatchingForLockedApplications() {
    foregroundDisposable = viewModel.onForegroundApplicationLockRequest(
        onEvent = { model: PadLockEntryModel, className: String, icon: Int ->
          LockScreenActivity.start(this, model, className, icon)
        },
        onError = { Timber.e(it, "Error while watching foreground applications") }
    )

    recheckDisposable = viewModel.onRecheckForcedLockEvent(
        onEvent = { model: PadLockEntryModel, className: String, icon: Int ->
          LockScreenActivity.start(this, model, className, icon)
        },
        onError = { Timber.e(it, "Error while attempting recheck") }
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    stopForeground(true)
    foregroundDisposable.tryDispose()
    recheckDisposable.tryDispose()
    screenStateDisposable.tryDispose()

    notificationManager.cancel(NOTIFICATION_ID)

    registry.fakeUnbind()

    PadLock.getRefWatcher(this)
        .watch(this)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    val command: Commands
    if (intent == null) {
      Timber.w("Intent was null, fallback to $START")
      command = START
    } else {
      val commandString = intent.getStringExtra(ServiceManager.SERVICE_COMMAND)
      command = Commands.valueOf(requireNotNull(commandString))
    }

    Timber.d("Service received command: $command")
    when (command) {
      START -> serviceStart()
      PAUSE -> servicePause()
      TEMP_PAUSE -> serviceTempPause()
      USER_PAUSE -> serviceUserPause()
      USER_TEMP_PAUSE -> serviceUserTempPause()
    }
    return Service.START_STICKY
  }

  private fun serviceStart() {
    Timber.d("System asked for service start")
    viewModel.setServicePaused(STARTED)

    // Cancel old notifications
    notificationManager.cancel(PAUSED_ID)
    notificationManager.cancel(PERMISSION_ID)

    // Cancel temporary intent
    jobSchedulerCompat.cancel(SERVICE_TEMP_PAUSE)

    // Start foreground service
    startForeground(NOTIFICATION_ID, notificationBuilder.build())
  }

  private fun servicePause() {
    Timber.d("System asked for service pause")
    // Paused by system, do not auto resume
    pauseService(false)
  }

  private fun serviceTempPause() {
    Timber.d("System asked for service temp pause")
    // Paused by system, auto resume later
    pauseService(true)
  }

  private fun serviceUserPause() {
    Timber.d("User asked for service pause")
    // Pause by user, check permission
    PauseConfirmActivity.start(applicationContext, autoResume = false)
  }

  private fun serviceUserTempPause() {
    Timber.d("User asked for service temp pause")
    // Auto resume pause by user, check permission
    PauseConfirmActivity.start(applicationContext, autoResume = true)
  }

  private fun pauseService(autoResume: Boolean) {
    Timber.d("Pause service with auto resume: $autoResume")
    viewModel.setServicePaused(if (autoResume) TEMP_PAUSED else PAUSED)

    if (autoResume) {
      // Queue the service to restart after timeout via alarm manager
      jobSchedulerCompat.queue(SERVICE_TEMP_PAUSE, TIME_TEMP_PAUSE)
    }

    // Stop the service here
    serviceStop()

    // But also create a paused notification
    createPausedNotification(autoResume)
  }

  private fun createPausedNotification(autoResume: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupPausedChannel()
    }

    var text = ""
    if (autoResume) {
      text = "Auto-Resume in $TEMP_PAUSE_AMOUNT minutes. "
    }

    text += "Tap to Resume now."
    val pausedNotificationBuilder = NotificationCompat.Builder(this, PAUSED_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_padlock_notification)
        .setColor(ContextCompat.getColor(applicationContext, R.color.blue500))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle("PadLock paused")
        .setContentText(text)
        .setContentIntent(serviceManager.startIntent())

    notificationManager.notify(PAUSED_ID, pausedNotificationBuilder.build())
  }

  private fun servicePermissionLost() {
    serviceStop()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupPermissionChannel()
    }

    val permissionNotificationBuilder = NotificationCompat.Builder(this, PERMISSION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_padlock_notification)
        .setColor(ContextCompat.getColor(applicationContext, R.color.blue500))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle("PadLock Missing Permissions")
        .setContentText("Tap to grant PadLock Usage Access permission")
        .setContentIntent(UsageAccessRequestDelegate.pendingIntent(this))

    notificationManager.notify(PERMISSION_ID, permissionNotificationBuilder.build())
  }

  private fun serviceStop() {
    Timber.d("Stopping service")
    stopForeground(true)
    stopSelf()
  }

  private fun setupNotifications() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupNotificationChannel()
    }

    notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentIntent(serviceManager.mainActivityIntent(false))
        .setColor(ContextCompat.getColor(applicationContext, R.color.blue500))
        .setSmallIcon(R.drawable.ic_padlock_notification)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setContentTitle(getString(R.string.app_name))
        .setContentText("PadLock service is monitoring applications")
        .addAction(
            R.drawable.ic_padlock_notification,
            "Pause",
            serviceManager.userPauseIntent()
        )
        .addAction(
            R.drawable.ic_padlock_notification,
            "Pause $TEMP_PAUSE_AMOUNT Minutes",
            serviceManager.tempPauseIntent()
        )
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun setupNotificationChannel() {
    val name = "PadLock Service"
    val desc = "Notification related to the PadLock service"
    val notificationChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID, name,
        NotificationManager.IMPORTANCE_MIN
    ).apply {
      description = desc
      enableLights(false)
      enableVibration(false)
      setShowBadge(false)
      setSound(null, null)
      importance = NotificationManager.IMPORTANCE_MIN
    }

    // Delete old unversioned channel
    if (notificationManager.getNotificationChannel(OLD_CHANNEL_ID) != null) {
      notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
    }

    Timber.d("Create notification channel with id: %s", notificationChannel.id)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun setupPausedChannel() {
    val name = "Paused PadLock Service"
    val desc = "Notification when PadLock service is paused"
    val notificationChannel = NotificationChannel(
        PAUSED_CHANNEL_ID, name,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = desc
      enableLights(false)
      enableVibration(false)
      setShowBadge(false)
      setSound(null, null)
      importance = NotificationManager.IMPORTANCE_DEFAULT
    }

    Timber.d("Create notification channel with id: %s", notificationChannel.id)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun setupPermissionChannel() {
    val name = "PadLock Permission Requests"
    val desc = "Notification when PadLock service requires permissions"
    val notificationChannel = NotificationChannel(
        PERMISSION_CHANNEL_ID, name,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = desc
      enableLights(false)
      enableVibration(false)
      setShowBadge(false)
      setSound(null, null)
      importance = NotificationManager.IMPORTANCE_DEFAULT
    }

    Timber.d("Create notification channel with id: %s", notificationChannel.id)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  companion object {

    private const val NOTIFICATION_ID = 1001
    private const val PAUSED_ID = 1002
    private const val PERMISSION_ID = 1003

    private const val OLD_CHANNEL_ID = "padlock_foreground"
    private const val NOTIFICATION_CHANNEL_ID = "padlock_foreground_v1"

    private const val PAUSED_CHANNEL_ID = "padlock_paused_v1"

    private const val PERMISSION_CHANNEL_ID = "padlock_permission_v1"

    private const val TEMP_PAUSE_AMOUNT = 30L
    private val TIME_TEMP_PAUSE = TimeUnit.MINUTES.toMillis(TEMP_PAUSE_AMOUNT)
  }
}
