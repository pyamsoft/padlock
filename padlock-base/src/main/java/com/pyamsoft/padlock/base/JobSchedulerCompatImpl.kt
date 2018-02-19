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

  override fun cancel(intent: Intent) {
    intent.toPending()
        .apply {
          alarmManager.cancel(this)
          cancel()
        }
  }

  override fun queue(
      intent: Intent,
      triggerTime: Long
  ) {
    cancel(intent)
    alarmManager.set(AlarmManager.RTC, triggerTime, intent.toPending())
  }
}
