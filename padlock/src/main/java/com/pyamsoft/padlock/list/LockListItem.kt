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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLocklistBinding
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.lifecycle.fakeBind
import com.pyamsoft.pydroid.core.lifecycle.fakeUnbind
import com.pyamsoft.pydroid.loader.ImageLoader
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

    private val binding: AdapterItemLocklistBinding = AdapterItemLocklistBinding.bind(itemView)
    private val lifecycle = LifecycleRegistry(this)

    @field:Inject internal lateinit var publisher: Publisher<LockListEvent>
    @field:Inject internal lateinit var imageLoader: ImageLoader
    @field:Inject internal lateinit var appIconLoader: AppIconLoader

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext)
          .inject(this)
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    fun bind(model: AppEntry) {
      binding.apply {
        lockListTitle.text = model.name
        lockListWhite.isInvisible = model.whitelisted.isEmpty()
        lockListLocked.isInvisible = model.hardLocked.isEmpty()

        // Must null out the old listener to avoid loops
        lockListToggle.setOnCheckedChangeListener(null)
        lockListToggle.isChecked = model.locked
      }

      if (binding.lockListWhite.isVisible) {
        imageLoader.load(R.drawable.ic_whitelisted)
            .into(binding.lockListWhite)
            .bind(this)
      }

      if (binding.lockListLocked.isVisible) {
        imageLoader.load(R.drawable.ic_hardlocked)
            .into(binding.lockListLocked)
            .bind(this)
      }

      if (binding.lockListIcon.isVisible) {
        appIconLoader.loadAppIcon(model.packageName, model.icon)
            .into(binding.lockListIcon)
            .bind(this)
      }

      binding.lockListToggle.setOnCheckedChangeListener { buttonView, isChecked ->
        buttonView.isChecked = isChecked.not()
        Timber.d("Modify the database entry: ${model.packageName} $isChecked")
        publisher.publish(LockListEvent(model.packageName, null, model.system, isChecked))
      }
      lifecycle.fakeBind()
    }

    fun unbind() {
      // All the visible fields are explicitly set in bind so we don't need to unbind them
      // We do want to clear out the lifecycle for any async processes though.
      lifecycle.fakeUnbind()
    }

  }
}
