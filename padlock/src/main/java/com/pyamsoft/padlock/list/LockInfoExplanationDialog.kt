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

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.pyamsoft.padlock.databinding.DialogInfoLocktypeExplainBinding
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled

class LockInfoExplanationDialog : ToolbarDialog() {

  private lateinit var binding: DialogInfoLocktypeExplainBinding

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = DialogInfoLocktypeExplainBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    binding.apply {
      lockInfoExplainToolbar.setUpEnabled(true)
      lockInfoExplainToolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
        dismiss()
      })
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.unbind()
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    dialog.window?.apply {
      setLayout(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT
      )
      setGravity(Gravity.CENTER)
    }
  }

  companion object {
    const val TAG = "LockInfoExplainationDialog"
  }
}

