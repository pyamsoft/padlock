package com.pyamsoft.padlock.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityPauseCheckBinding
import com.pyamsoft.padlock.pin.PinDialog
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.ui.app.activity.ActivityBase
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PauseConfirmActivity : ActivityBase() {

  @field:Inject internal lateinit var viewModel: PauseServiceViewModel
  @field:Inject internal lateinit var pausePublisher: Publisher<ServicePauseEvent>

  private lateinit var binding: ActivityPauseCheckBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_PadLock_Light_Transparent)
    overridePendingTransition(0, 0)
    super.onCreate(savedInstanceState)
    Timber.d("Launch with intent: $intent")
    binding = DataBindingUtil.setContentView(this, R.layout.activity_pause_check)

    Injector.obtain<PadLockComponent>(application)
        .plusServiceComponent(ServiceModule(this))
        .inject(this)

    viewModel.onCheckPinEventFailed {
      Snackbreak.make(
          binding.pauseCheckRoot,
          "Invalid PIN",
          Snackbar.LENGTH_SHORT
      )
          .show()
    }
    viewModel.onCheckPinEventSuccess {
      val autoResume = intent.getBooleanExtra(EXTRA_AUTO_RESUME, false)
      Timber.d("Pausing service with auto resume: $autoResume")
      pausePublisher.publish(ServicePauseEvent(autoResume))
      finish()
    }

    addPinFragment()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    Timber.d("New intent received: $intent")
    setIntent(intent)
  }

  private fun addPinFragment() {
    PinDialog.newInstance(checkOnly = true, finishOnDismiss = true)
        .show(this, PinDialog.TAG)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun onDestroy() {
    super.onDestroy()
    overridePendingTransition(0, 0)
    binding.unbind()
  }

  companion object {

    private const val EXTRA_AUTO_RESUME = "extra_auto_resume"

    @JvmStatic
    fun start(
      context: Context,
      autoResume: Boolean
    ) {
      val appContext = context.applicationContext
      val intent = Intent(appContext, PauseConfirmActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(EXTRA_AUTO_RESUME, autoResume)
      }
      appContext.startActivity(intent)
    }
  }
}
