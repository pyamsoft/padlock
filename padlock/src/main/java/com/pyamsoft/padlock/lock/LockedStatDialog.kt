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

  override fun provideBoundPresenters(): List<Presenter<*, *>> = emptyList()

  private lateinit var displayedLabel: String
  private lateinit var activityName: String
  private lateinit var packageName: String
  private lateinit var realName: String
  private lateinit var image: Bitmap
  private lateinit var binding: DialogLockStatBinding
  private var system: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    displayedLabel = arguments.getString(LABEL)
    packageName = arguments.getString(PKG_NAME)
    activityName = arguments.getString(ACT_NAME)
    realName = arguments.getString(REAL_NAME)
    system = arguments.getBoolean(SYSTEM)
    image = arguments.getParcelable(IMAGE)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = DialogLockStatBinding.inflate(LayoutInflater.from(activity), null, false)

    return AlertDialog.Builder(activity).setView(binding.root)
        .setPositiveButton("Okay") { dialogInterface, _ -> dialogInterface.dismiss() }
        .setCancelable(true)
        .create()
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? = binding.root

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.statImage.setImageBitmap(image)
    binding.statDisplayName.text = displayedLabel
    binding.statPackageName.text = packageName
    binding.statRealName.text = realName
    binding.statLockedBy.text = activityName
    binding.statSystem.text = if (system) "Yes" else "No"
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
      val fragment = LockedStatDialog()
      val args = Bundle()
      args.putString(LABEL, displayedLabel)
      args.putString(PKG_NAME, packageName)
      args.putString(ACT_NAME, activityName)
      args.putString(REAL_NAME, realName)
      args.putBoolean(SYSTEM, system)

      if (drawable is BitmapDrawable) {
        val bitmap = drawable.bitmap
        args.putParcelable(IMAGE, bitmap)
      }

      fragment.arguments = args
      return fragment
    }
  }
}
