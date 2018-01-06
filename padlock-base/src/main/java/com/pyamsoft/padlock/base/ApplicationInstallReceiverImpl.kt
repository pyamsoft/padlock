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

package com.pyamsoft.padlock.base

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
import android.support.v4.content.ContextCompat
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.api.PackageLabelManager
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.helper.enforceIo
import com.pyamsoft.pydroid.helper.enforceMainThread
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class ApplicationInstallReceiverImpl @Inject internal constructor(
        private val appContext: Context,
        private val packageManagerWrapper: PackageLabelManager,
        @param:Named("io") private val ioScheduler: Scheduler,
        @param:Named("main") private val mainThreadScheduler: Scheduler,
        @Named("main_activity") mainActivityClass: Class<out Activity>,
        @param:Named(
                "cache_purge") private val purgeCache: Cache,
        @param:Named(
                "cache_app_icons") private val iconCache: Cache,
        @param:Named(
                "cache_lock_list") private val listCache: Cache,
        @param:Named(
                "cache_lock_info") private val infoCache: Cache) : BroadcastReceiver(),
        ApplicationInstallReceiver {

    private val notificationChannelId: String = "padlock_new_apps"
    private val notificationManager: NotificationManager
    private val filter: IntentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
    private val pendingIntent: PendingIntent
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var notificationId: Int = NOTIFICATION_ID_START
        get() {
            if (field == NOTIFICATION_ID_MAX) {
                field = NOTIFICATION_ID_START
            } else {
                field += 1
            }

            return field
        }
    private var registered: Boolean = false

    init {
        filter.addDataScheme("package")
        val intent = Intent(appContext, mainActivityClass)
        intent.putExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST, true)
        pendingIntent = PendingIntent.getActivity(appContext, 421, intent, 0)
        notificationManager = appContext.getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager

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
        val importance = NotificationManager.IMPORTANCE_MIN
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
                    Timber.e(it, "onError launching notification for package: %s",
                            packageName)
                }))
    }

    private fun onNewPackageInstalled(packageName: String,
            name: String) {
        Timber.i("Package Added: %s", packageName)
        val builder = NotificationCompat.Builder(appContext, notificationChannelId).apply {
            setContentTitle("Lock New Application")
            setSmallIcon(R.drawable.ic_lock_notification)
            setContentText("Click to lock the newly installed application: " + name)
            setContentIntent(pendingIntent)
            setAutoCancel(true)
            color = ContextCompat.getColor(appContext, R.color.blue500)
            priority = NotificationCompat.PRIORITY_LOW
        }
        notificationManager.notify(notificationId++, builder.build())
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

    companion object {
        const val NOTIFICATION_ID_START = 2000
        const val NOTIFICATION_ID_MAX = 10000
    }
}
