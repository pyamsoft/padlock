package com.pyamsoft.padlock.list

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.AdapterItemLockinfoGroupBinding
import com.pyamsoft.padlock.model.list.ActivityEntry.Group
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

internal class LockInfoGroupViewImpl @Inject internal constructor(
  itemView: View,
  theming: Theming
) : LockInfoGroupView {

  private val binding = AdapterItemLockinfoGroupBinding.bind(itemView)

  init {
    val color: Int
    if (theming.isDarkTheme()) {
      color = R.color.dark_lock_group_background
    } else {
      color = R.color.lock_group_background
    }

    binding.root.background = ColorDrawable(ContextCompat.getColor(itemView.context, color))
  }

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
