package com.pyamsoft.padlock.list

import android.view.View
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoGroupBinding
import com.pyamsoft.padlock.model.list.ActivityEntry.Group
import javax.inject.Inject

internal class LockInfoGroupViewImpl @Inject internal constructor(itemView: View) : LockInfoGroupView {

  private val binding = AdapterItemLockinfoGroupBinding.bind(itemView)

  override fun bind(
    model: Group,
    packageName: String
  ) {
    binding.apply {
      val text: String
      val modelName = model.name
      if (modelName != packageName && modelName.startsWith(packageName)) {
        text = modelName.replaceFirst(packageName, "")
      } else {
        text = modelName
      }
      lockInfoGroupName.text = text
    }
  }

  override fun unbind() {
    binding.apply {
      lockInfoGroupName.text = null
      unbind()
    }
  }

}
