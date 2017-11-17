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

package com.pyamsoft.padlock.base.wrapper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class JobSchedulerCompatImpl @Inject internal constructor(
        context: Context) : JobSchedulerCompat {

    private val appContext: Context = context.applicationContext
    private val alarmManager: AlarmManager = context.applicationContext.getSystemService(
            Context.ALARM_SERVICE) as AlarmManager

    override fun cancel(intent: Intent) {
        val pendingIntent = PendingIntent.getService(appContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    override fun queue(intent: Intent, triggerTime: Long) {
        alarmManager.set(AlarmManager.RTC, triggerTime,
                PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
    }
}
