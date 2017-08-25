/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.list

import android.databinding.DataBindingUtil
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import com.mikepenz.fastadapter.items.GenericAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLocklistBinding
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.uicommon.AppIconLoader
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderMap
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockListItem internal constructor(internal var activity: FragmentActivity,
    entry: AppEntry) : GenericAbstractItem<AppEntry, LockListItem, LockListItem.ViewHolder>(
    entry), FilterableItem<LockListItem, LockListItem.ViewHolder> {

  private val loaderMap = LoaderMap()

  override fun getType(): Int = R.id.adapter_lock_item

  override fun getLayoutRes(): Int = R.layout.adapter_item_locklist

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  override fun filterAgainst(query: String): Boolean {
    val name = model.name().toLowerCase().trim { it <= ' ' }
    Timber.d("Filter predicate: '%s' against %s", query, name)
    return !name.startsWith(query)
  }

  override fun bindView(holder: ViewHolder, payloads: List<Any>?) {
    super.bindView(holder, payloads)
    holder.binding.lockListTitle.text = model.name()
    holder.binding.lockListToggle.setOnCheckedChangeListener(null)
    holder.binding.lockListToggle.isChecked = model.locked()

    val appIcon = ImageLoader.fromLoader(
        AppIconLoader.forPackageName(holder.itemView.context, model.packageName()))
        .into(holder.binding.lockListIcon)
    loaderMap.put("locked", appIcon)

    holder.binding.lockListToggle.setOnCheckedChangeListener(object : OnCheckedChangeListener {
      override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        buttonView.isChecked = !isChecked

        // Unset the listener here so that it is not double bound
        buttonView.setOnCheckedChangeListener(null)

        Timber.d("Modify the database entry: ${model.packageName()} $isChecked")
        holder.presenter.modifyDatabaseEntry(isChecked, model.packageName(), null,
            model.system(), onDatabaseEntryCreated = {
          Timber.d("Entry created for ${model.packageName()}")
          updateModel(true)
          buttonView.isChecked = true
        }, onDatabaseEntryDeleted = {
          Timber.d("Entry deleted for ${model.packageName()}")
          updateModel(false)
          buttonView.isChecked = false
        }, onComplete = {
          // Rebind the listener again once we complete
          buttonView.setOnCheckedChangeListener(this)
        }, onDatabaseEntryError = {
          DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "error")
        })
      }
    })

    holder.presenter.create(Unit)
    holder.presenter.start(Unit)
  }

  private fun updateModel(locked: Boolean) {
    withModel(model.toBuilder().locked(locked).build())
  }

  override fun unbindView(holder: ViewHolder?) {
    super.unbindView(holder)
    if (holder != null) {
      holder.binding.lockListTitle.text = null
      holder.binding.lockListIcon.setImageDrawable(null)
      holder.binding.lockListToggle.setOnCheckedChangeListener(null)
      holder.presenter.stop()
      holder.presenter.destroy()
    }

    loaderMap.clear()
  }

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val binding: AdapterItemLocklistBinding = DataBindingUtil.bind(itemView)
    @Inject internal lateinit var presenter: LockListItemPresenter

    init {
      Injector.with(itemView.context) {
        it.inject(this)
      }
    }

  }
}
