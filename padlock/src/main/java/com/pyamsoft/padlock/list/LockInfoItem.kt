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

import android.content.res.ColorStateList
import android.databinding.DataBindingUtil
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatRadioButton
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.RadioButton
import com.mikepenz.fastadapter.items.GenericAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding
import com.pyamsoft.padlock.list.info.LockInfoItemPublisher
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import timber.log.Timber
import javax.inject.Inject

class LockInfoItem internal constructor(entry: ActivityEntry,
    private val system: Boolean) : GenericAbstractItem<ActivityEntry, LockInfoItem, LockInfoItem.ViewHolder>(
    entry), FilterableItem<LockInfoItem, LockInfoItem.ViewHolder> {

  override fun getType(): Int = R.id.adapter_lock_info

  override fun getLayoutRes(): Int = R.layout.adapter_item_lockinfo

  override fun bindView(holder: ViewHolder, payloads: List<Any>?) {
    super.bindView(holder, payloads)

    val whitelistButton: RadioButton = holder.binding.lockInfoRadioWhite
    if (whitelistButton is AppCompatRadioButton) {
      whitelistButton.supportButtonTintList = holder.stateListWhitelisted
    }

    val blacklistButton: RadioButton = holder.binding.lockInfoRadioBlack
    if (blacklistButton is AppCompatRadioButton) {
      blacklistButton.supportButtonTintList = holder.stateListBlacklisted
    }

    // Remove any old binds
    val lockedButton: RadioButton = when (model.lockState) {
      DEFAULT -> holder.binding.lockInfoRadioDefault
      WHITELISTED -> holder.binding.lockInfoRadioWhite
      LOCKED -> holder.binding.lockInfoRadioBlack
      else -> throw IllegalStateException("Illegal enum state")
    }
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null)
    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null)
    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null)
    lockedButton.isChecked = true

    val entryName = model.name
    var activityName = entryName
    if (entryName.startsWith(model.packageName)) {
      val strippedPackageName = entryName.replace(model.packageName, "")
      if (strippedPackageName[0] == '.') {
        activityName = strippedPackageName
      }
    }
    holder.binding.lockInfoActivity.text = activityName

    holder.binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener { radioGroup, _ ->
      val id = radioGroup.checkedRadioButtonId
      Timber.d("Checked radio id: %d", id)
      if (id == 0) {
        Timber.e("No radiobutton is checked, set to default")
        holder.binding.lockInfoRadioDefault.isChecked = true
      }
    }

    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        processModifyDatabaseEntry(holder, DEFAULT)
      }
    }

    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        processModifyDatabaseEntry(holder, WHITELISTED)
      }
    }
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        processModifyDatabaseEntry(holder, LOCKED)
      }
    }
  }

  private fun processModifyDatabaseEntry(holder: ViewHolder, newLockState: LockState) {
    holder.publisher.publish(model, newLockState, null, system)
  }

  override fun unbindView(holder: ViewHolder?) {
    super.unbindView(holder)
    if (holder != null) {
      holder.binding.lockInfoActivity.text = null
      holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null)
      holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null)
      holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null)
      holder.binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener(null)
    }
  }

  override fun filterAgainst(query: String): Boolean {
    val name = model.name.toLowerCase().trim { it <= ' ' }
    Timber.d("Filter predicate: '%s' against %s", query, name)
    return !name.contains(query)
  }

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val binding: AdapterItemLockinfoBinding = DataBindingUtil.bind(itemView)
    @Inject internal lateinit var publisher: LockInfoItemPublisher
    internal val stateListWhitelisted: ColorStateList
    internal val stateListBlacklisted: ColorStateList

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext).inject(this)
      stateListWhitelisted = ContextCompat.getColorStateList(itemView.context,
          R.color.state_list_whitelisted)
      stateListBlacklisted = ContextCompat.getColorStateList(itemView.context,
          R.color.state_list_hardlocked)
    }
  }
}
