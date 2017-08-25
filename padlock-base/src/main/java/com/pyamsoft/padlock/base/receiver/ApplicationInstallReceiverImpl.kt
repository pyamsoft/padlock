/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.base.receiver

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import com.pyamsoft.padlock.base.R
import com.pyamsoft.padlock.base.wrapper.PackageLabelManager
import com.pyamsoft.pydroid.helper.enforceComputation
import com.pyamsoft.pydroid.helper.enforceIo
import com.pyamsoft.pydroid.helper.enforceMainThread
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class ApplicationInstallReceiverImpl @Inject internal constructor(
    context: Context,
    private val packageManagerWrapper: PackageLabelManager,
    @param:Named("computation") private val computationScheduler: Scheduler,
    @param:Named("io") private val ioScheduler: Scheduler,
    @param:Named("main") private val mainThreadScheduler: Scheduler,
    @Named(
        "main_activity") mainActivityClass: Class<out Activity>) : BroadcastReceiver(), ApplicationInstallReceiver {

  private val notificationChannelId: String = "padlock_new_apps"
  private val appContext: Context = context.applicationContext
  private val notificationManager: NotificationManager
  private val filter: IntentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
  private val pendingIntent: PendingIntent
  private val compositeDisposable: CompositeDisposable = CompositeDisposable()
  private var notificationId: Int = 0
  private var registered: Boolean = false

  init {
    filter.addDataScheme("package")
    val intent = Intent(appContext, mainActivityClass)
    intent.putExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST, true)
    pendingIntent = PendingIntent.getActivity(appContext, 421, intent, 0)
    notificationManager = appContext.getSystemService(
        Context.NOTIFICATION_SERVICE) as NotificationManager

    computationScheduler.enforceComputation()
    ioScheduler.enforceIo()
    mainThreadScheduler.enforceMainThread()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupNotificationChannel(notificationChannelId)
    }
  }

  @RequiresApi(VERSION_CODES.O) private fun setupNotificationChannel(
      notificationChannelId: String) {
    val name = "App Lock Suggestions"
    val description = "Suggestions to secure newly installed applications with PadLock"
    val importance = NotificationManagerCompat.IMPORTANCE_MIN
    val notificationChannel = NotificationChannel(notificationChannelId, name, importance)
    notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    notificationChannel.description = description
    notificationChannel.enableLights(false)
    notificationChannel.enableVibration(false)

    Timber.d("Create notification channel with id: %s", notificationChannelId)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) {
      Timber.e("NULL Intent")
      return
    }

    val isNew = !intent.hasExtra(Intent.EXTRA_REPLACING)
    val data = intent.data
    val packageName = data.schemeSpecificPart

    compositeDisposable.add(packageManagerWrapper.loadPackageLabel(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({ s ->
          if (isNew) {
            onNewPackageInstalled(packageName, s)
          } else {
            Timber.d("Package updated: %s", packageName)
          }
        }) { throwable ->
          Timber.e(throwable, "onError launching notification for package: %s",
              packageName)
        })
  }

  private fun onNewPackageInstalled(packageName: String,
      name: String) {
    Timber.i("Package Added: %s", packageName)
    val notification1 = NotificationCompat.Builder(appContext,
        notificationChannelId).setContentTitle(
        "Lock New Application")
        .setSmallIcon(R.drawable.ic_notification_lock)
        .setColor(ContextCompat.getColor(appContext, R.color.blue500))
        .setContentText("Click to lock the newly installed application: " + name)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(notificationId++, notification1)
  }

  override fun register() {
    if (!registered) {
      appContext.registerReceiver(this, filter)
      registered = true
    }
  }

  override fun unregister() {
    if (registered) {
      appContext.unregisterReceiver(this)
      registered = false
      compositeDisposable.clear()
    }
  }
}
