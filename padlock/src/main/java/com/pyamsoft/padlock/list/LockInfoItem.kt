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
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.RadioButton
import com.mikepenz.fastadapter.items.GenericAbstractItem
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding
import com.pyamsoft.padlock.list.info.LockInfoItemPresenter
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
    // Remove any old binds
    val lockedButton: RadioButton = when (model.lockState()) {
      DEFAULT -> holder.binding.lockInfoRadioDefault
      WHITELISTED -> holder.binding.lockInfoRadioWhite
      LOCKED -> holder.binding.lockInfoRadioBlack
      else -> throw IllegalStateException("Illegal enum state")
    }
    holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null)
    holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null)
    holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null)
    lockedButton.isChecked = true

    val entryName = model.name()
    var activityName = entryName
    if (entryName.startsWith(model.packageName())) {
      val strippedPackageName = entryName.replace(model.packageName(), "")
      if (strippedPackageName[0] == '.') {
        activityName = strippedPackageName
      }
    }
    holder.binding.lockInfoActivity.text = activityName

    holder.binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener { radioGroup, i ->
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

    holder.presenter.start(Unit)
  }

  private fun processModifyDatabaseEntry(holder: ViewHolder, newLockState: LockState) {
    holder.presenter.publish(model, newLockState, null, system)
  }

  override fun unbindView(holder: ViewHolder?) {
    super.unbindView(holder)
    if (holder != null) {
      holder.binding.lockInfoActivity.text = null
      holder.binding.lockInfoRadioBlack.setOnCheckedChangeListener(null)
      holder.binding.lockInfoRadioWhite.setOnCheckedChangeListener(null)
      holder.binding.lockInfoRadioDefault.setOnCheckedChangeListener(null)
      holder.binding.lockInfoTristateRadiogroup.setOnCheckedChangeListener(null)
      holder.presenter.stop()
    }
  }

  override fun filterAgainst(query: String): Boolean {
    val name = model.name().toLowerCase().trim { it <= ' ' }
    Timber.d("Filter predicate: '%s' against %s", query, name)
    return !name.contains(query)
  }

  override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val binding: AdapterItemLockinfoBinding = DataBindingUtil.bind(itemView)
    @Inject internal lateinit var presenter: LockInfoItemPresenter

    init {
      Injector.with(itemView.context) {
        it.inject(this)
      }
    }
  }
}
