package com.pyamsoft.padlock.service

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
import com.pyamsoft.padlock.model.service.RecheckEvent
import com.pyamsoft.pydroid.core.bus.Publisher
import timber.log.Timber
import javax.inject.Inject

class PadLockJobService : JobService() {

  @field:Inject internal lateinit var recheckBus: Publisher<RecheckEvent>
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
      recheckBus.publish(RecheckEvent(packageName, className))
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
