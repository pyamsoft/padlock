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

package com.pyamsoft.padlock.service.job

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.PersistableBundle
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLock
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.api.service.JobSchedulerCompat
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType.RECHECK
import com.pyamsoft.padlock.api.service.JobSchedulerCompat.JobType.SERVICE_TEMP_PAUSE
import com.pyamsoft.padlock.model.service.Recheck
import com.pyamsoft.padlock.service.RecheckPresenter
import com.pyamsoft.padlock.api.service.ServiceManager
import timber.log.Timber
import javax.inject.Inject

class PadLockJobService : JobService() {

  @field:Inject internal lateinit var presenter: RecheckPresenter
  @field:Inject internal lateinit var serviceManager: ServiceManager

  override fun onCreate() {
    super.onCreate()
    Injector.obtain<PadLockComponent>(applicationContext)
        .inject(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    PadLock.getRefWatcher(this)
        .watch(this)
  }

  override fun onStopJob(params: JobParameters): Boolean {
    // Returns false to indicate do NOT reschedule
    return false
  }

  private fun handleServicePauseJob() {
    Timber.d("Restarting service")
    serviceManager.startService(true)
  }

  private fun handleRecheckJob(extras: PersistableBundle) {
    val packageName = requireNotNull(extras.getString(Recheck.EXTRA_PACKAGE_NAME))
    val className = requireNotNull(extras.getString(Recheck.EXTRA_CLASS_NAME))

    if (packageName.isNotBlank() && className.isNotBlank()) {
      Timber.d("Recheck requested for $packageName $className")
      presenter.recheck(packageName, className)
    }
  }

  override fun onStartJob(params: JobParameters): Boolean {
    val extras = params.extras

    val typeName = extras.getString(JobSchedulerCompat.KEY_JOB_TYPE)
    val type = JobSchedulerCompat.JobType.valueOf(requireNotNull(typeName))
    when (type) {
      SERVICE_TEMP_PAUSE -> handleServicePauseJob()
      RECHECK -> handleRecheckJob(extras)
    }

    // Returns false to indicate this runs on main thread synchronously
    return false
  }

}
