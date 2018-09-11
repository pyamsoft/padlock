package com.pyamsoft.padlock.service

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.main.MainActivity
import com.pyamsoft.pydroid.core.lifecycle.fakeBind
import com.pyamsoft.pydroid.core.lifecycle.fakeUnbind
import timber.log.Timber
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class PauseService : IntentService(PauseService::class.java.name), LifecycleOwner {

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

    Timber.d("Pause service pausing PadLock")
    viewModel.setServicePaused(true)
    startPausedNotification()
    PadLockService.stop(applicationContext)
  }

  private fun startPausedNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      setupPausedChannel()
    }

    val launchMain = Intent(applicationContext, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val pe = PendingIntent.getActivity(applicationContext, PAUSED_RC, launchMain, 0)

    val resumeService = Intent(applicationContext, ResumeService::class.java)
    val resumePending = PendingIntent.getService(applicationContext, PAUSED_RC, resumeService, 0)

    val builder = NotificationCompat.Builder(applicationContext, PAUSED_CHANNEL_ID)
        .apply {
          setContentIntent(pe)
          setSmallIcon(R.drawable.ic_padlock_notification)
          setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          setContentTitle(getString(R.string.app_name))
          setContentText("PadLock Service is paused")
          color = ContextCompat.getColor(applicationContext, R.color.blue500)
          priority = NotificationCompat.PRIORITY_DEFAULT
          addAction(R.drawable.ic_padlock_notification, "Resume", resumePending)
        }

    notificationManager.cancel(PadLockService.NOTIFICATION_ID)
    notificationManager.notify(PAUSED_ID, builder.build())
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun setupPausedChannel() {
    val name = "Paused PadLock Service"
    val desc = "Notification related to the PadLock service"
    val notificationChannel = NotificationChannel(
        PAUSED_CHANNEL_ID, name,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = desc
      enableLights(false)
      enableVibration(false)
      setShowBadge(false)
      setSound(null, null)
      importance = NotificationManager.IMPORTANCE_DEFAULT
    }

    Timber.d("Create notification channel with id: %s", notificationChannel.id)
    notificationManager.createNotificationChannel(notificationChannel)
  }

  override fun onDestroy() {
    super.onDestroy()
    lifecycle.fakeUnbind()
  }

  companion object {

    const val PAUSED_ID = 1002
    private const val PAUSED_RC = 1004
    private const val PAUSED_CHANNEL_ID = "padlock_paused_v1"
  }

}

