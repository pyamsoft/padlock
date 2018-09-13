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
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class JobSchedulerCompatImpl @Inject internal constructor(
  private val context: Context
) : JobSchedulerCompat {

  private val alarmManager by lazy {
    requireNotNull(context.getSystemService<AlarmManager>())
  }

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

  override fun cancel(pendingIntent: PendingIntent) {
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
    triggerAfter: Long
  ) {
    val pendingIntent = createIntent(targetClass, params).toPending()
    queue(pendingIntent, triggerAfter)
  }

  override fun queue(
    pendingIntent: PendingIntent,
    triggerAfter: Long
  ) {
    cancel(pendingIntent)
    val time = System.currentTimeMillis() + triggerAfter
    alarmManager.set(AlarmManager.RTC, time, pendingIntent)
  }
}
