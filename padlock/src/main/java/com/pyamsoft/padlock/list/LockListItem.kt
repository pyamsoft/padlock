/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.list

import android.databinding.DataBindingUtil
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import com.mikepenz.fastadapter.items.GenericAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLocklistBinding
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.uicommon.AppIconLoader
import com.pyamsoft.pydroid.loader.LoaderMap
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
    val name = model.name.toLowerCase().trim { it <= ' ' }
    Timber.d("Filter predicate: '%s' against %s", query, name)
    return !name.startsWith(query)
  }

  override fun bindView(holder: ViewHolder, payloads: List<Any>?) {
    super.bindView(holder, payloads)
    holder.binding.lockListTitle.text = model.name
    holder.binding.lockListToggle.setOnCheckedChangeListener(null)
    holder.binding.lockListToggle.isChecked = model.locked

    val appIcon = AppIconLoader.forPackageName(holder.itemView.context, model.packageName).into(
        holder.binding.lockListIcon)
    loaderMap.put("locked", appIcon)

    holder.binding.lockListToggle.setOnCheckedChangeListener { buttonView, isChecked ->
      buttonView.isChecked = isChecked.not()
      Timber.d("Modify the database entry: ${model.packageName} $isChecked")
      holder.publisher.modifyDatabaseEntry(isChecked, model.packageName, null, model.system)
    }
  }

  override fun unbindView(holder: ViewHolder?) {
    super.unbindView(holder)
    if (holder != null) {
      holder.binding.lockListTitle.text = null
      holder.binding.lockListIcon.setImageDrawable(null)
      holder.binding.lockListToggle.setOnCheckedChangeListener(null)
    }

    loaderMap.clear()
  }

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val binding: AdapterItemLocklistBinding = DataBindingUtil.bind(itemView)
    @Inject internal lateinit var publisher: LockListItemPublisher

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext).inject(this)
    }

  }
}
