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

package com.pyamsoft.padlock.purge

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

internal class PurgeItemView @Inject internal constructor(
  parent: ViewGroup
) : BaseUiView<Unit>(parent, Unit) {

  override val layout: Int = R.layout.adapter_item_purge

  override val layoutRoot by lazyView<FrameLayout>(R.id.item_purge_root)

  private val itemName by lazyView<TextView>(R.id.item_purge_name)

  fun bind(model: String) {
    itemName.text = model
  }

  override fun onTeardown() {
    unbind()
  }

  fun unbind() {
    itemName.text = ""
  }

}