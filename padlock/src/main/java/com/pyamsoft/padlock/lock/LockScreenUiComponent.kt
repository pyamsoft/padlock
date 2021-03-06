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

import androidx.constraintlayout.widget.ConstraintLayout
import com.pyamsoft.pydroid.arch.UiComponent

internal interface LockScreenUiComponent : UiComponent<LockScreenUiComponent.Callback> {

  fun layout(
    constraintLayout: ConstraintLayout,
    aboveId: Int
  )

  fun clearForeground()

  fun checkUnlocked()

  // TODO(model should not be in view/component
  fun submit(
    attempt: String,
    isExcluded: Boolean,
    ignoreTime: Long
  )

  interface Callback {

    fun onClose()

    fun onSubmitAttempt(attempt: String)

  }

}