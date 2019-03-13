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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.util.fakeBind
import com.pyamsoft.pydroid.util.fakeUnbind
import timber.log.Timber
import javax.inject.Inject

class LockListItem internal constructor(
  internal var activity: FragmentActivity,
  entry: AppEntry
) : ModelAbstractItem<AppEntry, LockListItem, LockListItem.ViewHolder>(entry),
    FilterableItem<LockListItem, LockListItem.ViewHolder> {

  override fun getType(): Int = R.id.adapter_lock_item

  override fun getLayoutRes(): Int = R.layout.adapter_item_locklist

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  override fun filterAgainst(query: String): Boolean {
    val name = model.name.toLowerCase()
        .trim { it <= ' ' }
    Timber.d("Filter predicate: '%s' against %s", query, name)
    return name.contains(query)
  }

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

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
      LifecycleOwner {

    private val lifecycle = LifecycleRegistry(this)

    @field:Inject internal lateinit var view: LockListItemView
    @field:Inject internal lateinit var publisher: EventBus<LockListEvent>

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext)
          .plusLockListItemComponent()
          .owner(this)
          .itemView(itemView)
          .build()
          .inject(this)
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    fun bind(model: AppEntry) {
      view.bind(model)

      view.onSwitchChanged {
        Timber.d("Modify the database entry: ${model.packageName} $it")
        publisher.publish(LockListEvent(model.packageName, null, model.system, it))
      }

      lifecycle.fakeBind()
    }

    fun unbind() {
      // All the visible fields are explicitly set in bind so we don't need to unbind them
      // We do want to clear out the lifecycle for any async processes though.
      view.unbind()
      lifecycle.fakeUnbind()
    }

  }
}
