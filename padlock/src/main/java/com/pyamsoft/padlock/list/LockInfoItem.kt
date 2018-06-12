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

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.RadioButton
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
  entry: ActivityEntry.Item,
  private val system: Boolean
) : LockInfoBaseItem<ActivityEntry.Item, LockInfoItem, LockInfoItem.ViewHolder>(entry) {

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

        lockInfoActivity.text = model.activity

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
    holder.publisher.modifyEntry(model, newLockState, null, system)
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

    internal val binding: AdapterItemLockinfoBinding = AdapterItemLockinfoBinding.bind(itemView)
    @field:Inject
    internal lateinit var publisher: LockInfoItemPublisher

    init {
      Injector.obtain<PadLockComponent>(itemView.context.applicationContext)
          .inject(this)
    }
  }
}
