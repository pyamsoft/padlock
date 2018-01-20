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

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.databinding.DataBindingUtil
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.AppIconLoader
import com.pyamsoft.padlock.databinding.AdapterItemLocklistBinding
import com.pyamsoft.padlock.lifecycle.fakeBind
import com.pyamsoft.padlock.lifecycle.fakeRelease
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.pydroid.loader.ImageLoader
import timber.log.Timber
import javax.inject.Inject

class LockListItem internal constructor(
    internal var activity: FragmentActivity,
    entry: AppEntry
) : ModelAbstractItem<AppEntry, LockListItem, LockListItem.ViewHolder>(
    entry
), FilterableItem<LockListItem, LockListItem.ViewHolder> {

    override fun getType(): Int = R.id.adapter_lock_item

    override fun getLayoutRes(): Int = R.layout.adapter_item_locklist

    override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

    override fun filterAgainst(query: String): Boolean {
        val name = model.name.toLowerCase().trim { it <= ' ' }
        Timber.d("Filter predicate: '%s' against %s", query, name)
        return name.startsWith(query)
    }

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.apply {
            binding.apply {
                lockListTitle.text = model.name
                lockListToggle.setOnCheckedChangeListener(null)
                lockListToggle.isChecked = model.locked
                lockListWhite.visibility = if (model.whitelisted > 0) View.VISIBLE else View.INVISIBLE
                lockListLocked.visibility = if (model.hardLocked > 0) View.VISIBLE else View.INVISIBLE
            }

            imageLoader.apply {
                fromResource(R.drawable.ic_whitelisted).into(binding.lockListWhite)
                    .bind(holder)
                fromResource(R.drawable.ic_hardlocked).into(binding.lockListLocked)
                    .bind(holder)
            }
            appIconLoader.forPackageName(model.packageName).into(binding.lockListIcon)
                .bind(holder)

            binding.lockListToggle.setOnCheckedChangeListener { buttonView, isChecked ->
                buttonView.isChecked = isChecked.not()
                Timber.d("Modify the database entry: ${model.packageName} $isChecked")
                publisher.modifyDatabaseEntry(isChecked, model.packageName, null, model.system)
            }

            bind()
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.apply {
            binding.apply {
                lockListTitle.text = null
                lockListIcon.setImageDrawable(null)
                lockListToggle.setOnCheckedChangeListener(null)
            }
            unbind()
        }
    }

    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        LifecycleOwner {

        internal val binding: AdapterItemLocklistBinding = DataBindingUtil.bind(itemView)
        @Inject
        internal lateinit var publisher: LockListItemPublisher
        @Inject
        internal lateinit var appIconLoader: AppIconLoader
        @Inject
        internal lateinit var imageLoader: ImageLoader
        private val lifecycle = LifecycleRegistry(this)

        init {
            Injector.obtain<PadLockComponent>(itemView.context.applicationContext).inject(this)
        }

        override fun getLifecycle(): Lifecycle = lifecycle

        fun bind() {
            lifecycle.fakeBind()
        }

        fun unbind() {
            lifecycle.fakeRelease()
        }

    }
}
