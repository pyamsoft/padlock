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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.PinDialog
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.ActivityBase
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PauseConfirmActivity : ActivityBase() {

  @field:Inject internal lateinit var pausePresenter: ServicePausePresenter
  @field:Inject internal lateinit var viewModel: PauseServiceViewModel
  @field:Inject internal lateinit var theming: Theming
  @field:Inject internal lateinit var pauseView: PauseView

  private var checkPinFailedDisposable by singleDisposable()
  private var checkPinSuccessDisposable by singleDisposable()
  private var recreateDisposable by singleDisposable()

  override val fragmentContainerId: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    overridePendingTransition(0, 0)

    Injector.obtain<PadLockComponent>(application)
        .plusPauseComponent()
        .activity(this)
        .build()
        .inject(this)

    if (theming.isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Transparent)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Transparent)
    }
    super.onCreate(savedInstanceState)
    pauseView.create()

    checkPinFailedDisposable = viewModel.onCheckPinEventFailed {
      pauseView.onCheckPinFailed()
    }
    checkPinSuccessDisposable = viewModel.onCheckPinEventSuccess {
      val autoResume = intent.getBooleanExtra(EXTRA_AUTO_RESUME, false)
      Timber.d("Pausing service with auto resume: $autoResume")
      pausePresenter.pause(autoResume)
      finish()
    }
    recreateDisposable = viewModel.onRecreateEvent { recreate() }

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
    checkPinFailedDisposable.tryDispose()
    checkPinSuccessDisposable.tryDispose()
    recreateDisposable.tryDispose()
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
