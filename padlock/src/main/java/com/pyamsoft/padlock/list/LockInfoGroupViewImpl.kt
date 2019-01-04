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

package com.pyamsoft.padlock.list

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoGroupBinding
import com.pyamsoft.padlock.model.list.ActivityEntry.Group
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

internal class LockInfoGroupViewImpl @Inject internal constructor(
  itemView: View,
  theming: Theming
) : LockInfoGroupView {

  private val binding = AdapterItemLockinfoGroupBinding.bind(itemView)

  init {
    val color: Int
    if (theming.isDarkTheme()) {
      color = R.color.dark_lock_group_background
    } else {
      color = R.color.lock_group_background
    }

    binding.root.background = ColorDrawable(ContextCompat.getColor(itemView.context, color))
  }

  override fun bind(
    model: Group,
    packageName: String
  ) {
    binding.apply {
      val text: String
      val modelName = model.name
      if (modelName != packageName && modelName.startsWith(packageName)) {
        text = modelName.replaceFirst(packageName, "")
      } else {
        text = modelName
      }
      lockInfoGroupName.text = text
    }
  }

  override fun unbind() {
    binding.apply {
      lockInfoGroupName.text = null
      unbind()
    }
  }

}
