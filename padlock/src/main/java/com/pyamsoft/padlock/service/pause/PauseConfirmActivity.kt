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

package com.pyamsoft.padlock.service.pause

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.pin.ConfirmPinPresenter
import com.pyamsoft.padlock.pin.PinConfirmDialog
import com.pyamsoft.padlock.service.ServiceActionPresenter
import com.pyamsoft.pydroid.ui.app.ActivityBase
import com.pyamsoft.pydroid.ui.theme.ThemeInjector
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PauseConfirmActivity : ActivityBase(), ConfirmPinPresenter.Callback {

  @field:Inject internal lateinit var confirmPinPresenter: ConfirmPinPresenter
  @field:Inject internal lateinit var actionPresenter: ServiceActionPresenter

  override val fragmentContainerId: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    overridePendingTransition(0, 0)
    if (ThemeInjector.obtain(applicationContext).isDarkTheme()) {
      setTheme(R.style.Theme_PadLock_Dark_Transparent)
    } else {
      setTheme(R.style.Theme_PadLock_Light_Transparent)
    }
    super.onCreate(savedInstanceState)
    setContentView(R.layout.layout_constraint)

    val layoutRoot = findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<PadLockComponent>(application)
        .plusPinComponent()
        .owner(this)
        .parent(layoutRoot)
        .build()
        .inject(this)

    confirmPinPresenter.bind(this)

    PinConfirmDialog.newInstance(true)
        .show(this, PinConfirmDialog.TAG)
  }

  override fun onConfirmPinBegin() {
  }

  override fun onConfirmPinSuccess(attempt: String) {
    Timber.d("Pin check succeeds!")
    val autoResume = intent.getBooleanExtra(EXTRA_AUTO_RESUME, false)
    Timber.d("Pausing service with auto resume: $autoResume")
    actionPresenter.requestPause(autoResume)
    finish()
  }

  override fun onConfirmPinFailure(attempt: String) {
  }

  override fun onConfirmPinComplete() {
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    Timber.d("New intent received: $intent")
    setIntent(intent)
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun onDestroy() {
    super.onDestroy()
    overridePendingTransition(0, 0)
    confirmPinPresenter.unbind()
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
