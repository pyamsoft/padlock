/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.lock

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.lock.screen.LockScreenViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import javax.inject.Inject

class LockedStatDialog : DialogFragment() {

  @field:Inject internal lateinit var statView: LockStatView
  @field:Inject internal lateinit var viewModel: LockScreenViewModel

  private var displayNameDisposable by singleDisposable()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Injector.obtain<LockScreenComponent>(requireActivity())
        .plusStatsComponent()
        .lifecycle(lifecycle)
        .inflater(LayoutInflater.from(activity))
        .build()
        .inject(this)

    statView.create()

    return AlertDialog.Builder(requireActivity())
        .setView(statView.root())
        .setPositiveButton("Okay") { _, _ -> dismiss() }
        .setCancelable(true)
        .create()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // We override this so that we get onViewCreated callback
    return statView.root()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    displayNameDisposable = viewModel.loadDisplayNameFromPackage { statView.setDisplayName(it) }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    displayNameDisposable.tryDispose()
  }

  companion object {

    @CheckResult
    @JvmStatic
    fun newInstance(): LockedStatDialog {
      return LockedStatDialog()
    }
  }
}
