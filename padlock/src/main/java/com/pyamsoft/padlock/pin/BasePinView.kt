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

package com.pyamsoft.padlock.pin

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.Snackbreak

internal abstract class BasePinView<C : Any> protected constructor(
  protected val owner: LifecycleOwner,
  parent: ViewGroup,
  callback: C,
  protected val isConfirmMode: Boolean
) : BaseUiView<C>(parent, callback), PinView {

  protected abstract val layoutRoot: ViewGroup

  @CheckResult
  protected abstract fun getAttempt(): String

  @CheckResult
  protected abstract fun getReConfirmAttempt(): String

  @CheckResult
  protected abstract fun getOptionalHint(): String

  fun showMessage(message: String) {
    clearDisplay()
    Snackbreak.bindTo(owner)
        .short(layoutRoot, message)
        .show()
  }

}