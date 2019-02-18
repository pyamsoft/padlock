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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

internal class PauseView @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup
) : BaseUiView<Unit>(parent, Unit), LifecycleObserver {

  private val frameView by lazyView<FrameLayout>(R.id.layout_frame)

  override val layout: Int = R.layout.layout_frame

  override fun id(): Int {
    return frameView.id
  }

  fun showPinFailed() {
    Snackbreak.bindTo(owner)
        .short(frameView, "Invalid PIN")
        .show()
  }

}