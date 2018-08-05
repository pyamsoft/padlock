/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoGroupBinding
import com.pyamsoft.padlock.model.list.ActivityEntry

class LockInfoGroup internal constructor(
  private val packageName: String,
  entry: ActivityEntry.Group
) : LockInfoBaseItem<ActivityEntry.Group, LockInfoGroup, LockInfoGroup.ViewHolder>(entry) {

  override fun getType(): Int = R.id.adapter_lock_group

  override fun getLayoutRes(): Int = R.layout.adapter_item_lockinfo_group

  override fun bindView(
    holder: ViewHolder,
    payloads: List<Any>
  ) {
    super.bindView(holder, payloads)
    holder.bind(model, packageName)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  override fun filterAgainst(query: String): Boolean {
    return true
  }

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding = AdapterItemLockinfoGroupBinding.bind(itemView)

    fun bind(
      model: ActivityEntry.Group,
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

    fun unbind() {
      binding.apply {
        lockInfoGroupName.text = null
      }
    }

  }
}
