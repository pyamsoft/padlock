package com.pyamsoft.padlock.service

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.pydroid.core.lifecycle.fakeBind
import com.pyamsoft.pydroid.core.lifecycle.fakeUnbind
import timber.log.Timber
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class ResumeService : IntentService(ResumeService::class.java.name), LifecycleOwner {

  private val lifecycle = LifecycleRegistry(this)

  private val notificationManager by lazy(NONE) {
    requireNotNull(application.getSystemService<NotificationManager>())
  }

  @field:Inject
  internal lateinit var viewModel: LockServiceViewModel

  override fun getLifecycle(): Lifecycle {
    return lifecycle
  }

  override fun onHandleIntent(intent: Intent?) {
    Injector.obtain<PadLockComponent>(applicationContext)
        .plusServiceComponent(ServiceModule(this))
        .inject(this)

    lifecycle.fakeBind()

    Timber.d("Resume service resuming PadLock")
    viewModel.setServicePaused(false)

    notificationManager.cancel(PauseService.PAUSED_ID)
    PadLockService.start(applicationContext)
  }

  override fun onDestroy() {
    super.onDestroy()
    lifecycle.fakeUnbind()
  }

}
