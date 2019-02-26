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

package com.pyamsoft.padlock.base.jobs

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType.RECHECK
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType.SERVICE_TEMP_PAUSE
import com.pyamsoft.padlock.model.service.Recheck
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class JobSchedulerCompatImpl @Inject internal constructor(
  private val context: Context,
  private val jobServiceClass: Class<out JobService>
) : JobSchedulerCompat {

  private val serviceComponent by lazy { ComponentName(context, jobServiceClass) }
  private val recheckCache by lazy { ConcurrentHashMap<String, Int>() }
  private val jobScheduler by lazy {
    context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
  }

  @CheckResult
  private fun generateRecheckKey(
    extras: Map<String, String>
  ): String {
    val packageName = requireNotNull(extras[Recheck.EXTRA_PACKAGE_NAME])
    val className = requireNotNull(extras[Recheck.EXTRA_CLASS_NAME])
    return "$packageName|$className"
  }

  @CheckResult
  private fun buildJobInfo(
    jobId: Int,
    type: JobType,
    triggerAfter: Long,
    extras: Map<String, String>
  ): JobInfo {
    val builder = JobInfo.Builder(jobId, serviceComponent)
        // Job will run anywhere from CURRENT + triggerTime
        .setMinimumLatency(triggerAfter)
        // Job will run by CURRENT + (2 * triggerTime)
        .setOverrideDeadline(2 * triggerAfter)
        // Do not persist across restart
        .setPersisted(false)
        // Bundle extras
        .setExtras(PersistableBundle().apply {
          putString(JobSchedulerCompat.KEY_JOB_TYPE, type.name)
          for ((key, value) in extras) {
            putString(key, value)
          }
        })

    return builder.build()
  }

  override fun cancelAll() {
    // Cancel all of the Recheck jobs but do not cancel the TEMP SERVICE job
    // since it will need to restart the service at a later point
    recheckCache.values.forEach { jobScheduler.cancel(it) }
    recheckCache.clear()
  }

  override fun cancel(
    type: JobType,
    extras: Map<String, String>
  ) {
    when (type) {
      // Service temp pause must have an active id to cancel
      SERVICE_TEMP_PAUSE -> {
        Timber.d("Cancel job for temp pause service")
        jobScheduler.cancel(SERVICE_PAUSE_JOB_ID)
      }
      RECHECK -> {
        // Check for key in cache map and cancel job
        val key = generateRecheckKey(extras)
        Timber.d("Cancel job for recheck: $key")
        recheckCache[key]?.also { jobScheduler.cancel(it) }
      }
    }
  }

  override fun queue(
    type: JobType,
    triggerAfter: Long,
    extras: Map<String, String>
  ) {
    // Cancel old jobs
    cancel(type, extras)

    val job: JobInfo
    when (type) {
      SERVICE_TEMP_PAUSE -> {
        Timber.d("Construct job for temp pause service")
        job = buildJobInfo(SERVICE_PAUSE_JOB_ID, type, triggerAfter, extras)
      }
      RECHECK -> {
        val key = generateRecheckKey(extras)
        Timber.d("Construct job for recheck: $key")
        job = buildJobInfo(key.hashCode(), type, triggerAfter, extras)
      }
    }

    jobScheduler.schedule(job)
  }

  companion object {

    private const val SERVICE_PAUSE_JOB_ID = 1

  }
}
