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
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.RadioButton
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding
import com.pyamsoft.padlock.list.info.LockInfoItemPublisher
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import timber.log.Timber
import javax.inject.Inject

class LockInfoItem internal constructor(
    entry: ActivityEntry,
    private val system: Boolean
) :
    ModelAbstractItem<ActivityEntry, LockInfoItem, LockInfoItem.ViewHolder>(
        entry
    ), FilterableItem<LockInfoItem, LockInfoItem.ViewHolder> {

  override fun getType(): Int = R.id.adapter_lock_info

  override fun getLayoutRes(): Int = R.layout.adapter_item_lockinfo

  override fun bindView(
      holder: ViewHolder,
      payloads: List<Any>
  ) {
    super.bindView(holder, payloads)
    holder.apply {
      binding.apply {
        // Remove any old binds
        val lockedButton: RadioButton = when (model.lockState) {
          LockState.DEFAULT -> lockInfoRadioDefault
          LockState.WHITELISTED -> lockInfoRadioWhite
          LockState.LOCKED -> lockInfoRadioBlack
          else -> throw IllegalStateException("Illegal enum state")
        }
        lockInfoRadioBlack.setOnCheckedChangeListener(null)
        lockInfoRadioWhite.setOnCheckedChangeListener(null)
        lockInfoRadioDefault.setOnCheckedChangeListener(null)
        lockedButton.isChecked = true

        lockInfoActivity.text = model.name

        lockInfoRadioGroup.setOnCheckedChangeListener { radioGroup, _ ->
          val id = radioGroup.checkedRadioButtonId
          Timber.d("Checked radio id: %d", id)
          if (id == 0) {
            Timber.e("No radiobutton is checked, set to default")
            lockInfoRadioDefault.isChecked = true
          }
        }

        lockInfoRadioDefault.setOnCheckedChangeListener { _, isChecked ->
          if (isChecked) {
            processModifyDatabaseEntry(holder, LockState.DEFAULT)
          }
        }
        lockInfoRadioWhite.setOnCheckedChangeListener { _, isChecked ->
          if (isChecked) {
            processModifyDatabaseEntry(holder, LockState.WHITELISTED)
          }
        }
        lockInfoRadioBlack.setOnCheckedChangeListener { _, isChecked ->
          if (isChecked) {
            processModifyDatabaseEntry(holder, LockState.LOCKED)
          }
        }
      }
    }
  }

  private fun processModifyDatabaseEntry(
      holder: ViewHolder,
      newLockState: LockState
  ) {
    holder.publisher.publish(model, newLockState, null, system)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.apply {
      binding.apply {
        lockInfoActivity.text = null
        lockInfoRadioBlack.setOnCheckedChangeListener(null)
        lockInfoRadioWhite.setOnCheckedChangeListener(null)
        lockInfoRadioDefault.setOnCheckedChangeListener(null)
        lockInfoRadioGroup.setOnCheckedChangeListener(null)
      }
    }
  }

  override fun filterAgainst(query: String): Boolean {
    val name = model.name.toLowerCase()
        .trim { it <= ' ' }
    Timber.d("Filter predicate: '%s' against %s", query, name)
    return name.contains(query)
  }

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val binding: AdapterItemLockinfoBinding = DataBindingUtil.bind(itemView)
    @Inject
    internal lateinit var publisher: LockInfoItemPublisher

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext)
          .inject(this)
    }
  }
}
