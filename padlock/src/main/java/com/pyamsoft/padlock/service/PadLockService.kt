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
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.lifecycle.fakeBind
import com.pyamsoft.padlock.lifecycle.fakeRelease
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.main.MainActivity
import timber.log.Timber
import javax.inject.Inject

class PadLockService : Service(), LifecycleOwner {

  private val lifecycle = LifecycleRegistry(this)

  override fun getLifecycle(): Lifecycle = lifecycle

  override fun onBind(ignore: Intent?): IBinder? {
    throw AssertionError("Service is not bound")
  }

  @field:Inject
  internal lateinit var viewModel: LockServiceViewModel
  private lateinit var notificationManagerCompat: NotificationManagerCompat
  private lateinit var notificationManager: NotificationManager
  private lateinit var handler: Handler

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext)
        .plusServiceComponent(ServiceModule(this))
        .inject(this)
    notificationManagerCompat = NotificationManagerCompat.from(this)
    notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    handler = Handler(Looper.getMainLooper())
    startInForeground()
    lifecycle.fakeBind()

    viewModel.onLockScreen { entry, realName ->
      // Delay by a little bit for Applications which launch a bunch of Activities in quick order.
      handler.postDelayed({ LockScreenActivity.start(this, entry, realName) }, LAUNCH_DELAY)
    }

    viewModel.onServiceFinishEvent { stopSelf() }
  }

  override fun onDestroy() {
    super.onDestroy()
    stopForeground(true)
    handler.removeCallbacksAndMessages(null)
    notificationManagerCompat.cancel(NOTIFICATION_ID)
    lifecycle.fakeRelease()
    PadLock.getRefWatcher(this)
        .watch(this)
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    Timber.d("Service onStartCommand")
    return Service.START_STICKY
  }

  private fun startInForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupNotificationChannel()
    }

    val launchMain = Intent(applicationContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val pe = PendingIntent.getActivity(applicationContext, NOTIFICATION_RC, launchMain, 0)
    val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        .apply {
          setContentIntent(pe)
          setSmallIcon(R.drawable.ic_padlock_notification)
          setOngoing(true)
          setWhen(0)
          setNumber(0)
          setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          setContentTitle(getString(R.string.app_name))
          setContentText("PadLock Service is running")
          color = ContextCompat.getColor(applicationContext, R.color.blue500)
          priority = NotificationCompat.PRIORITY_MIN
        }
    startForeground(NOTIFICATION_ID, builder.build())
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

  companion object {

    private const val NOTIFICATION_ID = 1001
    private const val NOTIFICATION_RC = 1004
    private const val OLD_CHANNEL_ID = "padlock_foreground"
    private const val NOTIFICATION_CHANNEL_ID = "padlock_foreground_v1"
    private const val LAUNCH_DELAY = 300L

    @JvmStatic
    fun start(context: Context) {
      val appContext = context.applicationContext
      val service = Intent(appContext, PadLockService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(service)
      } else {
        appContext.startService(service)
      }
    }
  }
}
