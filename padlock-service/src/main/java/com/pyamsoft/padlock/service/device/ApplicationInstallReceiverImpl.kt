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

package com.pyamsoft.padlock.service.device

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.api.packagemanager.PackageLabelManager
import com.pyamsoft.padlock.api.service.ServiceManager
import com.pyamsoft.pydroid.core.cache.Cache
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class ApplicationInstallReceiverImpl @Inject internal constructor(
  private val context: Context,
  private val packageManagerWrapper: PackageLabelManager,
  private val serviceManager: ServiceManager,
  @Named("notification_color") @ColorRes private val notificationColor: Int,
  @Named("notification_icon") @DrawableRes private val notificationIcon: Int,
  @param:Named("cache_purge") private val purgeCache: Cache,
  @param:Named("cache_lock_list") private val listCache: Cache,
  @param:Named("cache_lock_info") private val infoCache: Cache
) : BroadcastReceiver(), ApplicationInstallReceiver {

  private val notificationManager: NotificationManager
  private val filter: IntentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
  private val compositeDisposable: CompositeDisposable = CompositeDisposable()
  private var registered: Boolean = false

  init {
    filter.addDataScheme("package")
    notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
    ).apply {
      description = desc
      enableLights(false)
      enableVibration(false)
      setShowBadge(true)
      setSound(null, null)
      importance = NotificationManager.IMPORTANCE_DEFAULT
    }

    // Delete old unversioned channel
    if (notificationManager.getNotificationChannel(OLD_CHANNEL_ID) != null) {
      notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
    }

    Timber.d("Create notification channel with id: %s", notificationChannel.id)
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
    val packageName = data?.schemeSpecificPart.orEmpty()

    compositeDisposable.add(
        packageManagerWrapper.loadPackageLabel(packageName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
              if (isNew) {
                purgeCache.clearCache()
                listCache.clearCache()
                infoCache.clearCache()
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
    val builder = NotificationCompat.Builder(
        context,
        NOTIFICATION_CHANNEL_ID
    )
        .apply {
          setContentTitle("Lock New Application")
          setSmallIcon(notificationIcon)
          setContentText("Click to lock the newly installed application: $name")
          setContentIntent(serviceManager.fireMainActivityIntent(true))
          setAutoCancel(true)
          color = ContextCompat.getColor(context, notificationColor)
          priority = NotificationCompat.PRIORITY_LOW
        }
    notificationManager.notify(
        notificationId++, builder.build()
    )
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
    private const val OLD_CHANNEL_ID: String = "padlock_new_apps"
    private const val NOTIFICATION_CHANNEL_ID: String = "padlock_new_apps_v1"
    private const val NOTIFICATION_ID_START = 2000
    private const val NOTIFICATION_ID_MAX = 10000

    private var notificationId: Int =
      NOTIFICATION_ID_START
      get() {
        if (field == NOTIFICATION_ID_MAX) {
          field =
            NOTIFICATION_ID_START
        } else {
          field += 1
        }

        return field
      }
  }
}
