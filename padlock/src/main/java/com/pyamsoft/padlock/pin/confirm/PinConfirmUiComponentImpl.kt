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

package com.pyamsoft.padlock.pin.confirm

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.ConfirmPinPresenter
import com.pyamsoft.padlock.pin.ConfirmPinView
import com.pyamsoft.padlock.pin.confirm.PinConfirmUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import javax.inject.Inject
import javax.inject.Named

internal class PinConfirmUiComponentImpl @Inject internal constructor(
  private val pinView: ConfirmPinView,
  private val presenter: PinConfirmDialogPresenter,
  private val confirmPresenter: ConfirmPinPresenter,
  @Named("finish_on_dismiss") private val finishOnDismiss: Boolean
) : BaseUiComponent<PinConfirmUiComponent.Callback>(),
    PinConfirmUiComponent,
    PinConfirmDialogPresenter.Callback,
    ConfirmPinPresenter.Callback {

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: Callback
  ) {
    owner.doOnDestroy {
      pinView.teardown()
      presenter.unbind()
      confirmPresenter.unbind()
    }

    pinView.inflate(savedInstanceState)
    presenter.bind(this)
    confirmPresenter.bind(this)
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

  override fun onAttemptSubmit(attempt: String) {
    confirmPresenter.confirm(attempt, checkOnly = finishOnDismiss)
  }

  override fun onConfirmPinBegin() {
    pinView.disable()
  }

  override fun onConfirmPinSuccess(
    attempt: String,
    checkOnly: Boolean
  ) {
    onPinCallback()
  }

  override fun onConfirmPinFailure(
    attempt: String,
    checkOnly: Boolean
  ) {
    onPinCallback()
  }

  override fun onConfirmPinComplete() {
    pinView.enable()
  }

  private fun onPinCallback() {
    pinView.clearDisplay()
    callback.onClose()
  }

}