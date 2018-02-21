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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.JobSchedulerCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class JobSchedulerCompatImpl @Inject internal constructor(
    private val context: Context
) : JobSchedulerCompat {

  private val alarmManager: AlarmManager =
      context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  @CheckResult
  private fun Intent.toPending(): PendingIntent = PendingIntent.getService(context, 0, this, 0)

  @CheckResult
  private fun createIntent(
      targetClass: Class<*>,
      params: List<Pair<String, String>>
  ): Intent {
    return Intent(context, targetClass).apply {
      for (pair in params) {
        putExtra(pair.first, pair.second)
      }
    }
  }

  private fun cancel(pendingIntent: PendingIntent) {
    pendingIntent.apply {
      alarmManager.cancel(this)
      cancel()
    }
  }

  override fun cancel(
      targetClass: Class<*>,
      params: List<Pair<String, String>>
  ) {
    val intent = createIntent(targetClass, params)
    cancel(intent.toPending())
  }

  override fun queue(
      targetClass: Class<*>,
      params: List<Pair<String, String>>,
      triggerTime: Long
  ) {
    val pendingIntent = createIntent(targetClass, params).toPending()
    cancel(pendingIntent)
    alarmManager.set(AlarmManager.RTC, triggerTime, pendingIntent)
  }
}
