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

package com.pyamsoft.padlock.purge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.R.layout
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

class PurgeFragment : Fragment(),
    PurgeUiComponent.Callback,
    PurgeToolbarUiComponent.Callback {

  @field:Inject internal lateinit var toolbarComponent: PurgeToolbarUiComponent
  @field:Inject internal lateinit var component: PurgeUiComponent

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(layout.layout_frame, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val layoutRoot = view.findViewById<ViewGroup>(R.id.layout_frame)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPurgeComponent()
        .toolbarActivity(requireToolbarActivity())
        .owner(viewLifecycleOwner)
        .parent(layoutRoot)
        .build()
        .inject(this)

    component.bind(viewLifecycleOwner, savedInstanceState, this)
    toolbarComponent.bind(viewLifecycleOwner, savedInstanceState, this)
  }

  override fun onPurgeErrorOccurred(throwable: Throwable) {
    component.showError(throwable)
  }

  override fun showPurgeAllConfirmation(stalePackages: List<String>) {
    PurgeAllDialog.newInstance(stalePackages)
        .show(requireActivity(), "purge_all")
  }

  override fun showPurgeSingleConfirmation(stalePackage: String) {
    PurgeSingleItemDialog.newInstance(stalePackage)
        .show(requireActivity(), "purge_single")
  }

  override fun onStart() {
    super.onStart()
    component.refresh(false)
  }

  override fun onPause() {
    super.onPause()
    component.saveListPosition()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    component.saveState(outState)
    toolbarComponent.saveState(outState)
  }

  companion object {

    const val TAG = "PurgeFragment"
  }
}
