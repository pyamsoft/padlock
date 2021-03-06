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

package com.pyamsoft.padlock.pin.text

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.pin.ConfirmPinView
import com.pyamsoft.padlock.pin.ConfirmPinView.Callback
import com.pyamsoft.pydroid.loader.ImageLoader
import javax.inject.Inject

internal class TextConfirmPinView @Inject internal constructor(
  imageLoader: ImageLoader,
  owner: LifecycleOwner,
  parent: ViewGroup,
  callback: Callback
) : ConfirmPinView, TextPinView<Callback>(imageLoader, owner, parent, callback, true) {

  override fun showHint(hint: String) {
    setHintText(hint)
  }

  override fun submit() {
    callback.onSubmit(getAttempt())
  }

}

