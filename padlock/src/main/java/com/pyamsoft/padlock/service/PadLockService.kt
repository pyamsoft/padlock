/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.support.annotation.CheckResult
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.boot.BootReceiver
import com.pyamsoft.padlock.lock.LockScreenActivity
import com.pyamsoft.padlock.main.MainActivity
import timber.log.Timber
import javax.inject.Inject

class PadLockService : Service(), LockServicePresenter.View {

  override fun onBind(ignore: Intent?): IBinder? = null

  @field:Inject internal lateinit var presenter: LockServicePresenter

  override fun onCreate() {
    super.onCreate()

    if (ContextCompat.checkSelfPermission(applicationContext,
        Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
      Timber.e("We do not have usage stats permission, killing service")
      stopSelf()
      return
    }

    Injector.obtain<PadLockComponent>(applicationContext).inject(this)
    presenter.bind(this)
    updateBootEnabledState()

    startInForeground()
    isRunning = true
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.unbind()
    isRunning = false
  }

  override fun onFinish() {
    stopForeground(true)
    stopSelf()

    // Update boot state here since this function is called by the app only, not the system
    updateBootEnabledState()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Timber.d("Service onStartCommand")
    return Service.START_STICKY
  }

  override fun onRecheck(packageName: String, className: String) {
    presenter.processActiveApplicationIfMatching(packageName, className)
  }

  override fun onStartLockScreen(entry: PadLockEntry, realName: String) {
    Timber.d("Start lock activity for entry: %s %s (real %s)", entry.packageName(),
        entry.activityName(), realName)
    LockScreenActivity.start(this, entry, realName)
  }

  private fun startInForeground() {
    val id = 1001
    val requestCode = 1004

    val notificationChannelId = "padlock_foreground"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupNotificationChannel(notificationChannelId)
    }

    val launchMain = Intent(applicationContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val pe = PendingIntent.getActivity(applicationContext, requestCode, launchMain,
        PendingIntent.FLAG_UPDATE_CURRENT)
    val n = NotificationCompat.Builder(applicationContext, notificationChannelId).apply {
      setContentIntent(pe)
      setSmallIcon(R.mipmap.ic_launcher)
      setOngoing(true)
      setWhen(0)
      setNumber(0)
      setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      priority = NotificationCompat.PRIORITY_MIN
      color = ContextCompat.getColor(applicationContext, R.color.blue500)
      setContentTitle(getString(R.string.app_name)).setContentText("PadLock Service is running")
    }.build()
    NotificationManagerCompat.from(applicationContext).notify(id, n)
  }

  @RequiresApi(Build.VERSION_CODES.O) private fun setupNotificationChannel(
      notificationChannelId: String) {
    val name = "PadLock Service"
    val description = "Notification related to the PadLock service"
    val importance = NotificationManager.IMPORTANCE_MIN
    val notificationChannel = NotificationChannel(notificationChannelId, name, importance)
    notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    notificationChannel.description = description
    notificationChannel.enableLights(false)
    notificationChannel.enableVibration(false)

    Timber.d("Create notification channel with id: %s", notificationChannelId)
    val notificationManager: NotificationManager = applicationContext.getSystemService(
        Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(notificationChannel)
  }

  private fun updateBootEnabledState() {
    val flag: Int
    if (isRunning) {
      flag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
      flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    application.let {
      val component = ComponentName(it, BootReceiver::class.java)
      it.packageManager.setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP)
    }
  }

  companion object {

    var isRunning: Boolean = false
      @CheckResult get
      private set

    @JvmStatic
    fun start(context: Context) {
      val appContext = context.applicationContext
      if (ContextCompat.checkSelfPermission(appContext,
          Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
        Timber.e("We do not have usage stats permission, do not start service")
        return
      }

      val service = Intent(appContext, PadLockService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(service)
      } else {
        appContext.startService(service)
      }
    }
  }
}
