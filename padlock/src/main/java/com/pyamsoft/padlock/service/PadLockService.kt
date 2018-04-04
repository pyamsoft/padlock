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
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import androidx.content.systemService
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.lifecycle.fakeBind
import com.pyamsoft.padlock.lifecycle.fakeRelease
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import timber.log.Timber
import javax.inject.Inject

class PadLockService : Service(), LockServicePresenter.View, LifecycleOwner {

  private val lifecycle = LifecycleRegistry(this)

  override fun getLifecycle(): Lifecycle = lifecycle

  override fun onBind(ignore: Intent?): IBinder? {
    throw AssertionError("Service is not bound")
  }

  @field:Inject
  internal lateinit var presenter: LockServicePresenter
  private lateinit var notificationManagerCompat: NotificationManagerCompat
  private lateinit var notificationManager: NotificationManager

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext)
        .inject(this)
    presenter.bind(this, this)
    notificationManagerCompat = NotificationManagerCompat.from(this)
    notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    startInForeground()
    lifecycle.fakeBind()
  }

  override fun onDestroy() {
    super.onDestroy()
    stopForeground(true)
    notificationManagerCompat.cancel(NOTIFICATION_ID)
    lifecycle.fakeRelease()
    PadLock.getRefWatcher(this)
        .watch(this)
  }

  override fun onFinish() {
    stopSelf()
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    Timber.d("Service onStartCommand")
    return Service.START_STICKY
  }

  override fun onRecheck(
    packageName: String,
    className: String
  ) {
    presenter.processActiveApplicationIfMatching(packageName, className)
  }

  override fun onStartLockScreen(
    entry: PadLockEntryModel,
    realName: String
  ) {
    LockScreenActivity.start(this, entry, realName)
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
        .also {
          it.setContentIntent(pe)
          it.setSmallIcon(R.drawable.ic_padlock_notification)
          it.setOngoing(true)
          it.setWhen(0)
          it.setNumber(0)
          it.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          it.setContentTitle(getString(R.string.app_name))
          it.setContentText("PadLock Service is running")
          it.color = ContextCompat.getColor(applicationContext, R.color.blue500)
          it.priority = NotificationCompat.PRIORITY_MIN
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
    ).also {
      it.description = desc
      it.enableLights(false)
      it.enableVibration(false)
      it.setShowBadge(false)
    }

    Timber.d("Create notification channel with id: %s", NOTIFICATION_CHANNEL_ID)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  companion object {

    private const val NOTIFICATION_ID = 1001
    private const val NOTIFICATION_RC = 1004
    private const val NOTIFICATION_CHANNEL_ID = "padlock_foreground"

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
