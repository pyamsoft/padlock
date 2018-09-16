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
import javax.inject.Inject

class PauseConfirmActivity : ActivityBase() {

  private var autoResume: Boolean = false
  @field:Inject internal lateinit var viewModel: PauseServiceViewModel
  @field:Inject internal lateinit var publisher: Publisher<ServicePauseEvent>

  private lateinit var binding: ActivityPauseCheckBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_PadLock_Light_Transparent)
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_pause_check)
    autoResume = intent.getBooleanExtra(EXTRA_AUTO_RESUME, false)

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
      publisher.publish(ServicePauseEvent(autoResume))
      finish()
    }

    addPinFragment()
  }

  private fun addPinFragment() {
    val fragmentManager = supportFragmentManager
    if (fragmentManager.findFragmentByTag(PinDialog.TAG) == null) {
      fragmentManager.beginTransaction()
          .replace(R.id.pause_check_root, PinDialog.newInstance(true), PinDialog.TAG)
          .commit()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
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
