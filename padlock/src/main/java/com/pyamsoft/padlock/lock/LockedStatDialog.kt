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

package com.pyamsoft.padlock.lock

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyamsoft.padlock.databinding.DialogLockStatBinding
import com.pyamsoft.padlock.uicommon.CanaryDialog

class LockedStatDialog : CanaryDialog() {

  private lateinit var displayedLabel: String
  private lateinit var activityName: String
  private lateinit var packageName: String
  private lateinit var realName: String
  private lateinit var image: Bitmap
  private lateinit var binding: DialogLockStatBinding
  private var system: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      displayedLabel = it.getString(LABEL)
      packageName = it.getString(PKG_NAME)
      activityName = it.getString(ACT_NAME)
      realName = it.getString(REAL_NAME)
      system = it.getBoolean(SYSTEM)
      image = it.getParcelable(IMAGE)
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = DialogLockStatBinding.inflate(LayoutInflater.from(activity), null, false)

    return AlertDialog.Builder(requireActivity())
        .setView(binding.root)
        .setPositiveButton("Okay") { dialogInterface, _ -> dialogInterface.dismiss() }
        .setCancelable(true)
        .create()
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? = binding.root

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    binding.apply {
      statImage.setImageBitmap(image)
      statDisplayName.text = displayedLabel
      statPackageName.text = packageName
      statRealName.text = realName
      statLockedBy.text = activityName
      statSystem.text = if (system) "Yes" else "No"
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.unbind()
  }

  companion object {

    private const val PKG_NAME = "pkgname"
    private const val ACT_NAME = "actname"
    private const val REAL_NAME = "realname"
    private const val SYSTEM = "system"
    private const val LABEL = "label"
    private const val IMAGE = "image"

    @CheckResult
    @JvmStatic
    fun newInstance(
        displayedLabel: String,
        packageName: String,
        activityName: String,
        realName: String,
        system: Boolean,
        drawable: Drawable
    ): LockedStatDialog {
      return LockedStatDialog().apply {
        arguments = Bundle().apply {
          putString(LABEL, displayedLabel)
          putString(PKG_NAME, packageName)
          putString(ACT_NAME, activityName)
          putString(REAL_NAME, realName)
          putBoolean(SYSTEM, system)
          if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            putParcelable(IMAGE, bitmap)
          }
        }
      }
    }
  }
}
