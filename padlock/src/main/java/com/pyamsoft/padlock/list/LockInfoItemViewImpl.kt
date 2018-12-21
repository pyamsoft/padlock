package com.pyamsoft.padlock.list

import android.view.View
import android.widget.RadioButton
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoBinding
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.model.list.ActivityEntry.Item
import timber.log.Timber
import javax.inject.Inject

internal class LockInfoItemViewImpl @Inject internal constructor(itemView: View) : LockInfoItemView {

  private val binding = AdapterItemLockinfoBinding.bind(itemView)

  override fun bind(
    model: Item,
    system: Boolean
  ) {
    binding.apply {
      // Remove any old binds
      val lockedButton: RadioButton = when (model.lockState) {
        LockState.DEFAULT -> lockInfoRadioDefault
        LockState.WHITELISTED -> lockInfoRadioWhite
        LockState.LOCKED -> lockInfoRadioBlack
        else -> throw IllegalStateException("Illegal enum state")
      }

      // Must null out the old listeners to avoid loops
      lockInfoRadioGroup.setOnCheckedChangeListener(null)
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

    }
  }

  override fun onSwitchChanged(onChange: (lockState: LockState) -> Unit) {
    binding.apply {
      lockInfoRadioDefault.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          onChange(DEFAULT)
        }
      }
      lockInfoRadioWhite.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          onChange(WHITELISTED)
        }
      }
      lockInfoRadioBlack.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          onChange(LOCKED)
        }
      }
    }
  }

  override fun unbind() {
    binding.unbind()
  }

}
