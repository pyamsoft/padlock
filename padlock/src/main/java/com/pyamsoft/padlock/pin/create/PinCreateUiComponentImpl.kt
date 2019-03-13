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

package com.pyamsoft.padlock.pin.create

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.CreatePinPresenter
import com.pyamsoft.padlock.pin.CreatePinView
import com.pyamsoft.padlock.pin.create.PinCreateUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import javax.inject.Inject

internal class PinCreateUiComponentImpl @Inject internal constructor(
  private val pinView: CreatePinView,
  private val presenter: PinCreateDialogPresenter,
  private val createPresenter: CreatePinPresenter
) : BaseUiComponent<PinCreateUiComponent.Callback>(),
    PinCreateUiComponent,
    PinCreateDialogPresenter.Callback,
    CreatePinPresenter.Callback {

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: Callback
  ) {
    owner.doOnDestroy {
      pinView.teardown()
      presenter.unbind()
      createPresenter.unbind()
    }

    pinView.inflate(savedInstanceState)
    presenter.bind(this)
    createPresenter.bind(this)
  }

  override fun saveState(outState: Bundle) {
    pinView.saveState(outState)
  }

  override fun layout(
    constraintLayout: ConstraintLayout,
    aboveId: Int
  ) {
    ConstraintSet().apply {
      clone(constraintLayout)

      pinView.also {
        connect(it.id(), ConstraintSet.TOP, aboveId, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, constraintLayout.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, constraintLayout.id, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      applyTo(constraintLayout)
    }
  }

  override fun submit() {
    pinView.submit()
  }

  override fun onAttemptSubmit(
    attempt: String,
    reEntry: String,
    hint: String
  ) {
    createPresenter.create(attempt, reEntry, hint)
  }

  override fun onCreatePinBegin() {
    pinView.disable()
  }

  override fun onCreatePinSuccess() {
    onPinCreateCallback()
  }

  override fun onCreatePinFailure() {
    onPinCreateCallback()
  }

  override fun onCreatePinComplete() {
    pinView.enable()
  }

  private fun onPinCreateCallback() {
    pinView.clearDisplay()
    callback.onClose()
  }

}