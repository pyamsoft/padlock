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
import com.pyamsoft.pydroid.presenter.Presenter

class LockedStatDialog : CanaryDialog() {

  override fun provideBoundPresenters(): List<Presenter<*>> = emptyList()

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

    return AlertDialog.Builder(activity!!).setView(binding.root)
        .setPositiveButton("Okay") { dialogInterface, _ -> dialogInterface.dismiss() }
        .setCancelable(true)
        .create()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? = binding.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

    const private val PKG_NAME = "pkgname"
    const private val ACT_NAME = "actname"
    const private val REAL_NAME = "realname"
    const private val SYSTEM = "system"
    const private val LABEL = "label"
    const private val IMAGE = "image"

    @CheckResult
    @JvmStatic
    fun newInstance(displayedLabel: String,
        packageName: String, activityName: String, realName: String,
        system: Boolean, drawable: Drawable): LockedStatDialog {
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
