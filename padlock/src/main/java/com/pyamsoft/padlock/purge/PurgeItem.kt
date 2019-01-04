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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import javax.inject.Inject

class PurgeItem internal constructor(
  packageName: String
) : ModelAbstractItem<String, PurgeItem, PurgeItem.ViewHolder>(
    packageName
) {

  override fun getType(): Int = R.id.adapter_purge

  override fun getLayoutRes(): Int = R.layout.adapter_item_purge

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  override fun bindView(
    holder: ViewHolder,
    payloads: List<Any>
  ) {
    super.bindView(holder, payloads)
    holder.bind(model)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @field:Inject lateinit var view: PurgeItemView

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext)
          .plusPurgeItemComponent()
          .itemView(itemView)
          .build()
          .inject(this)
    }

    fun bind(model: String) {
      view.bind(model)
    }

    fun unbind() {
      view.unbind()
    }
  }
}
