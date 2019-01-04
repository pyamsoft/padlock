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