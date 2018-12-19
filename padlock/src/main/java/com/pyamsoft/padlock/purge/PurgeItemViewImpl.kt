package com.pyamsoft.padlock.purge

import android.view.View
import com.pyamsoft.padlock.databinding.AdapterItemPurgeBinding
import javax.inject.Inject

internal class PurgeItemViewImpl @Inject internal constructor(
  view: View
) : PurgeItemView {

  private val binding = AdapterItemPurgeBinding.bind(view)

  override fun bind(model: String) {
    binding.itemPurgeName.text = model
  }

  override fun unbind() {
  }

}