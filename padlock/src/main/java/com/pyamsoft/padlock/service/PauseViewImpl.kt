package com.pyamsoft.padlock.service

import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityPauseCheckBinding
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

internal class PauseViewImpl @Inject internal constructor(
  private val activity: PauseConfirmActivity
) : PauseView, LifecycleObserver {

  private lateinit var binding: ActivityPauseCheckBinding

  init {
    activity.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    activity.lifecycle.removeObserver(this)

    binding.unbind()
  }

  override fun create() {
    binding = DataBindingUtil.setContentView(activity, R.layout.activity_pause_check)
  }

  override fun onCheckPinFailed() {
    Snackbreak.short(binding.pauseCheckRoot, "Invalid PIN")
        .show()
  }

}