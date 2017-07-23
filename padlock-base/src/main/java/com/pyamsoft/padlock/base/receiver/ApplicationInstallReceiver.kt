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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.NotificationCompat
import com.pyamsoft.padlock.base.R
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper
import com.pyamsoft.pydroid.helper.SchedulerHelper
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton class ApplicationInstallReceiver @Inject internal constructor(context: Context,
    private val packageManagerWrapper: PackageManagerWrapper,
    @param:Named("obs") private val obsScheduler: Scheduler,
    @param:Named("sub") private val subScheduler: Scheduler,
    @Named("main") mainActivityClass: Class<out Activity>) : BroadcastReceiver() {

  private val appContext: Context = context.applicationContext
  private val notificationManager: NotificationManagerCompat
  private val filter: IntentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
  private val pendingIntent: PendingIntent
  private val compositeDisposable: CompositeDisposable = CompositeDisposable()
  private var notificationId: Int = 0
  private var registered: Boolean = false

  init {
    filter.addDataScheme("package")
    pendingIntent = PendingIntent.getActivity(appContext, 421,
        Intent(appContext, mainActivityClass), 0)
    notificationManager = NotificationManagerCompat.from(appContext)

    SchedulerHelper.enforceForegroundScheduler(obsScheduler)
    SchedulerHelper.enforceBackgroundScheduler(subScheduler)
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
        .subscribeOn(subScheduler)
        .observeOn(obsScheduler)
        .subscribe({
          if (isNew) {
            onNewPackageInstalled(packageName, it)
          } else {
            Timber.d("Package updated: %s", packageName)
          }
        }, {
          Timber.e(it, "onError launching notification for package: %s",
              packageName)
        }))
  }

  internal fun onNewPackageInstalled(packageName: String,
      name: String) {
    Timber.i("Package Added: %s", packageName)
    val notification1 = NotificationCompat.Builder(appContext).setContentTitle(
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

  fun register() {
    if (!registered) {
      appContext.registerReceiver(this, filter)
      registered = true
    }
  }

  fun unregister() {
    if (registered) {
      appContext.unregisterReceiver(this)
      registered = false
      compositeDisposable.clear()
    }
  }
}
