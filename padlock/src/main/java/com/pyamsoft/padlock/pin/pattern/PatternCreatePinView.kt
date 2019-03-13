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

package com.pyamsoft.padlock.pin.pattern

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.CreatePinView
import com.pyamsoft.padlock.pin.CreatePinView.Callback
import javax.inject.Inject

internal class PatternCreatePinView @Inject internal constructor(
  owner: LifecycleOwner,
  parent: ViewGroup,
  callback: Callback,
  @ColorRes normalDotColor: Int
) : CreatePinView,
    PatternPinView<Callback>(owner, parent, callback, false, normalDotColor) {

  override fun submit() {
    val attempt = getAttempt()
    if (isRepeating()) {
      val repeatAttempt = getReConfirmAttempt()
      submitPinCreate(attempt, repeatAttempt)
    } else {
      submitPinReConfirm(attempt)
    }
  }

  @CheckResult
  private fun fail(
    attempt: String,
    repeatAttempt: String
  ): Boolean {
    val failed = attempt.isBlank() || repeatAttempt.isBlank()
    if (failed) {
      callback.onSubmit("this will", "intentionally fail", "try again")
    }

    return failed
  }

  private fun submitPinReConfirm(attempt: String) {
    if (attempt.isBlank() || attempt.length < MINIMUM_PATTERN_LENGTH) {
      showMessage("Please create a PIN code")
    } else {
      commitAndPromptRepeat()
    }
  }

  private fun submitPinCreate(
    attempt: String,
    repeatAttempt: String
  ) {
    // If we don't have an entry yet, fail out
    if (fail(attempt, repeatAttempt)) {
      return
    }

    callback.onSubmit(attempt, repeatAttempt, getOptionalHint())
  }

}