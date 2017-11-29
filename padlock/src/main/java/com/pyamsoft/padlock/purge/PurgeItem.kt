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

package com.pyamsoft.padlock.purge

import android.support.v7.widget.RecyclerView
import android.view.View
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemPurgeBinding

internal class PurgeItem internal constructor(
        packageName: String) : ModelAbstractItem<String, PurgeItem, PurgeItem.ViewHolder>(
        packageName) {

    override fun getType(): Int = R.id.adapter_purge

    override fun getLayoutRes(): Int = R.layout.adapter_item_purge

    override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.apply {
            binding.itemPurgeName.text = null
        }
    }

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.apply {
            binding.itemPurgeName.text = model
        }
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val binding: AdapterItemPurgeBinding = AdapterItemPurgeBinding.bind(itemView)

    }
}
