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
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.ConfirmPinView.Callback
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

internal class TextConfirmPinView @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup,
  callback: Callback
) : ConfirmPinView, TextPinView<Callback>(parent, callback, true) {

  override fun submit() {
    callback.onSubmit(getAttempt())
  }

  override fun showPinError() {
    Snackbreak.bindTo(owner)
        .short(layoutRoot, "Invalid PIN")
        .show()
  }

}

