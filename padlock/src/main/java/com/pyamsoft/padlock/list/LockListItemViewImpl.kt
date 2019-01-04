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

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLocklistBinding
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.loader.ImageLoader
import javax.inject.Inject

internal class LockListItemViewImpl @Inject internal constructor(
  itemView: View,
  private val imageLoader: ImageLoader,
  private val appIconLoader: AppIconLoader,
  private val owner: LifecycleOwner
) : LockListItemView {

  private val binding = AdapterItemLocklistBinding.bind(itemView)

  override fun bind(model: AppEntry) {
    binding.apply {
      lockListTitle.text = model.name
      lockListWhite.isInvisible = model.whitelisted.isEmpty()
      lockListLocked.isInvisible = model.hardLocked.isEmpty()

      // Must null out the old listener to avoid loops
      lockListToggle.setOnCheckedChangeListener(null)
      lockListToggle.isChecked = model.locked

      if (lockListWhite.isVisible) {
        imageLoader.load(R.drawable.ic_whitelisted)
            .into(lockListWhite)
            .bind(owner)
      }

      if (lockListLocked.isVisible) {
        imageLoader.load(R.drawable.ic_hardlocked)
            .into(lockListLocked)
            .bind(owner)
      }

      if (lockListIcon.isVisible) {
        appIconLoader.loadAppIcon(model.packageName, model.icon)
            .into(lockListIcon)
            .bind(owner)
      }
    }
  }

  override fun onSwitchChanged(onChange: (isChecked: Boolean) -> Unit) {
    binding.lockListToggle.setOnCheckedChangeListener { buttonView, isChecked ->
      buttonView.isChecked = !isChecked
      onChange(isChecked)
    }
  }

  override fun unbind() {
    binding.apply {
      lockListToggle.setOnCheckedChangeListener(null)
      unbind()
    }
  }

}
