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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.ConfirmPinView
import com.pyamsoft.padlock.service.ForegroundEventPresenter
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class LockScreenUiComponentImpl @Inject internal constructor(
  private val iconView: LockImageView,
  private val pinScreen: ConfirmPinView,
  private val presenter: LockScreenPresenter,
  private val foregroundPresenter: ForegroundEventPresenter,
  @Named("locked_package_name") private val packageName: String,
  @Named("locked_activity_name") private val activityName: String,
  @Named("locked_real_name") private val realName: String,
  @Named("locked_code") private val lockedCode: String?,
  @Named("locked_system") private val lockedSystem: Boolean
) : BaseUiComponent<LockScreenUiComponent.Callback>(),
    LockScreenUiComponent,
    LockScreenPresenter.Callback {

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: LockScreenUiComponent.Callback
  ) {
    owner.doOnDestroy {
      iconView.teardown()
      pinScreen.teardown()
      presenter.unbind()
    }

    iconView.inflate(savedInstanceState)
    pinScreen.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun saveState(outState: Bundle) {
    pinScreen.saveState(outState)
    iconView.saveState(outState)
  }

  override fun layout(
    constraintLayout: ConstraintLayout,
    aboveId: Int
  ) {
    ConstraintSet().apply {
      clone(constraintLayout)

      iconView.also {
        connect(it.id(), ConstraintSet.TOP, aboveId, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, constraintLayout.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, constraintLayout.id, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      pinScreen.also {
        connect(it.id(), ConstraintSet.TOP, iconView.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, constraintLayout.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, constraintLayout.id, ConstraintSet.END)
        connect(it.id(), ConstraintSet.BOTTOM, constraintLayout.id, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      applyTo(constraintLayout)
    }

  }

  override fun checkUnlocked() {
    presenter.checkUnlocked()
  }

  override fun clearForeground() {
    foregroundPresenter.foreground(packageName, realName)
  }

  override fun onCloseOld() {
    Timber.w("Close event received for this old lock screen: $this")
    callback.onClose()
  }

  override fun onShowLockHint(hint: String) {
    pinScreen.showHint(hint)
  }

  override fun onSubmitUnlockAttempt(attempt: String) {
    callback.onSubmitAttempt(attempt)
  }

  override fun submit(
    attempt: String,
    isExcluded: Boolean,
    ignoreTime: Long
  ) {
    Timber.d("Attempt unlock submission")
    presenter.submit(lockedCode, attempt, lockedSystem, isExcluded, ignoreTime)
  }

  override fun onAlreadyUnlocked() {
    Timber.d("$packageName $activityName unlocked, close lock screen")
    callback.onClose()
  }

  override fun onSubmitBegin() {
    Timber.d("Submit begin")
    pinScreen.disable()
  }

  override fun onSubmitUnlocked() {
    Timber.d("Unlocked! $packageName $activityName $realName")
    pinScreen.clearDisplay()
    callback.onClose()
  }

  override fun onSubmitLocked() {
    Timber.w("Temp Locked! $packageName $activityName $realName")
    pinScreen.clearDisplay()
    pinScreen.enable()
    pinScreen.showErrorMessage("Error: This App is temporarily locked.")
  }

  override fun onSubmitFailed() {
    Timber.w("Failed unlock! $packageName $activityName $realName")
    pinScreen.clearDisplay()
    pinScreen.enable()
    pinScreen.showErrorMessage("Error: Invalid PIN")
  }

  override fun onSubmitError(throwable: Throwable) {
    pinScreen.clearDisplay()
    pinScreen.enable()
    pinScreen.showErrorMessage("Something went wrong during PIN submission, please try again")
  }

}