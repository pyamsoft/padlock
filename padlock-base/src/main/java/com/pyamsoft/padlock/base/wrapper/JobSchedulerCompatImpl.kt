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

package com.pyamsoft.padlock.base.wrapper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import javax.inject.Inject

internal class JobSchedulerCompatImpl @Inject constructor(context: Context) : JobSchedulerCompat {

  private val appContext: Context = context.applicationContext
  private val alarmManager: AlarmManager = context.applicationContext.getSystemService(
      Context.ALARM_SERVICE) as AlarmManager

  override fun cancel(intent: Intent) {
    val pendingIntent = PendingIntent.getService(appContext, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT)
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
  }

  override fun set(intent: Intent, triggerTime: Long) {
    alarmManager.set(AlarmManager.RTC, triggerTime,
        PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
  }
}
