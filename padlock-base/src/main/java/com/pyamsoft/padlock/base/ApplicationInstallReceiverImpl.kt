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

package com.pyamsoft.padlock.base

import android.app.Activity
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
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.api.PackageLabelManager
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.data.enforceIo
import com.pyamsoft.pydroid.data.enforceMainThread
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class ApplicationInstallReceiverImpl @Inject internal constructor(
  private val context: Context,
  private val packageManagerWrapper: PackageLabelManager,
  @param:Named("io") private val ioScheduler: Scheduler,
  @param:Named("main") private val mainThreadScheduler: Scheduler,
  @Named("main_activity") mainActivityClass: Class<out Activity>,
  @param:Named("cache_purge") private val purgeCache: Cache,
  @param:Named("cache_app_icons") private val iconCache: Cache,
  @param:Named("cache_lock_list") private val listCache: Cache,
  @param:Named("cache_lock_info") private val infoCache: Cache
) : BroadcastReceiver(), ApplicationInstallReceiver {

  private val notificationManager: NotificationManager
  private val notificationManagerCompat: NotificationManagerCompat
  private val filter: IntentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
  private val pendingIntent: PendingIntent
  private val compositeDisposable: CompositeDisposable = CompositeDisposable()
  private var registered: Boolean = false

  init {
    filter.addDataScheme("package")
    val intent = Intent(context, mainActivityClass).apply {
      putExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST, true)
    }
    pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_RC, intent, 0)
    notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    ioScheduler.enforceIo()
    mainThreadScheduler.enforceMainThread()

    notificationManagerCompat = NotificationManagerCompat.from(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupNotificationChannel()
    }
  }

  @RequiresApi(VERSION_CODES.O)
  private fun setupNotificationChannel() {
    val name = "App Lock Suggestions"
    val desc = "Suggestions to secure newly installed applications with PadLock"
    val notificationChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID, name,
        NotificationManager.IMPORTANCE_DEFAULT
    ).also {
      it.description = desc
      it.enableLights(false)
      it.enableVibration(false)
    }

    Timber.d("Create notification channel with id: %s", NOTIFICATION_CHANNEL_ID)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  override fun onReceive(
    context: Context,
    intent: Intent?
  ) {
    if (intent == null) {
      Timber.e("NULL Intent")
      return
    }

    val isNew = !intent.hasExtra(Intent.EXTRA_REPLACING)
    val data = intent.data
    val packageName = data.schemeSpecificPart

    compositeDisposable.add(
        packageManagerWrapper.loadPackageLabel(packageName)
            .subscribeOn(ioScheduler)
            .observeOn(mainThreadScheduler)
            .subscribe({
              if (isNew) {
                purgeCache.clearCache()
                listCache.clearCache()
                infoCache.clearCache()
                iconCache.clearCache()
                onNewPackageInstalled(packageName, it)
              } else {
                Timber.d("Package updated: %s", packageName)
              }
            }, {
              Timber.e(
                  it, "onError launching notification for package: %s",
                  packageName
              )
            })
    )
  }

  private fun onNewPackageInstalled(
    packageName: String,
    name: String
  ) {
    Timber.i("Package Added: %s", packageName)
    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .also {
          it.setContentTitle("Lock New Application")
          it.setSmallIcon(R.drawable.ic_lock_notification)
          it.setContentText("Click to lock the newly installed application: $name")
          it.setContentIntent(pendingIntent)
          it.setAutoCancel(true)
          it.priority = NotificationCompat.PRIORITY_LOW
          it.color = ContextCompat.getColor(context, R.color.blue500)
        }
    notificationManagerCompat.notify(notificationId++, builder.build())
  }

  override fun register() {
    if (!registered) {
      context.registerReceiver(this, filter)
      registered = true
    }
  }

  override fun unregister() {
    if (registered) {
      context.unregisterReceiver(this)
      registered = false
      compositeDisposable.clear()
    }
  }

  companion object {
    private const val NOTIFICATION_CHANNEL_ID: String = "padlock_new_apps"
    private const val NOTIFICATION_RC = 421
    private const val NOTIFICATION_ID_START = 2000
    private const val NOTIFICATION_ID_MAX = 10000

    private var notificationId: Int = NOTIFICATION_ID_START
      get() {
        if (field == NOTIFICATION_ID_MAX) {
          field = NOTIFICATION_ID_START
        } else {
          field += 1
        }

        return field
      }
  }
}
