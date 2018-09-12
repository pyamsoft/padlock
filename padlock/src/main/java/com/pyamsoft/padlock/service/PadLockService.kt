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
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.service.ServiceManager.Commands
import com.pyamsoft.padlock.service.ServiceManager.Commands.PAUSE
import com.pyamsoft.padlock.service.ServiceManager.Commands.START
import com.pyamsoft.padlock.service.ServiceManager.Commands.STOP
import com.pyamsoft.padlock.service.ServiceManager.Commands.TEMP_PAUSE
import com.pyamsoft.padlock.uicommon.UsageAccessRequestDelegate
import com.pyamsoft.pydroid.core.lifecycle.fakeBind
import com.pyamsoft.pydroid.core.lifecycle.fakeUnbind
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class PadLockService : Service(), LifecycleOwner {

  private val lifecycle = LifecycleRegistry(this)
  private val notificationManager by lazy(NONE) {
    requireNotNull(application.getSystemService<NotificationManager>())
  }

  private lateinit var notificationBuilder: NotificationCompat.Builder

  @field:Inject
  internal lateinit var viewModel: LockServiceViewModel
  @field:Inject
  internal lateinit var serviceManager: ServiceManager
  @field:Inject
  internal lateinit var jobSchedulerCompat: JobSchedulerCompat

  override fun getLifecycle(): Lifecycle = lifecycle

  override fun onBind(ignore: Intent?): IBinder? {
    throw AssertionError("Service is not bound")
  }

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext)
        .plusServiceComponent(ServiceModule(this))
        .inject(this)
    lifecycle.fakeBind()

    setupNotifications()

    viewModel.onLockScreen { entry, realName, icon ->
      // Delay by a little bit for Applications which launch a bunch of Activities in quick order.
      LockScreenActivity.start(this, entry, realName, icon)
    }

    viewModel.onServiceFinishEvent { serviceStop() }

    viewModel.onPermissionLostEvent { servicePermissionLost() }

  }

  override fun onDestroy() {
    super.onDestroy()
    stopForeground(true)
    notificationManager.cancel(NOTIFICATION_ID)
    lifecycle.fakeUnbind()
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

    when (command) {
      START -> serviceStart()
      PAUSE -> servicePause(false)
      STOP -> serviceStop()
      TEMP_PAUSE -> serviceTempPause()
    }
    return Service.START_STICKY
  }

  private fun serviceStart() {
    viewModel.setServicePaused(false)
    // Cancel old notifications
    notificationManager.cancel(PAUSED_ID)
    notificationManager.cancel(PERMISSION_ID)

    // Cancel temporary intent
    jobSchedulerCompat.cancel(serviceManager.startIntent())
    startForeground(NOTIFICATION_ID, notificationBuilder.build())
  }

  private fun serviceTempPause() {
    // Queue the service to restart after timeout via alarm manager
    jobSchedulerCompat.queue(
        serviceManager.startIntent(), System.currentTimeMillis() + TIME_TEMP_PAUSE
    )

    servicePause(true)
  }

  private fun servicePause(autoResume: Boolean) {
    viewModel.setServicePaused(true)

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
            serviceManager.pauseIntent()
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
