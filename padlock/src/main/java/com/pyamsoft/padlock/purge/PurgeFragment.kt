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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PurgeFragment : Fragment(),
    PurgeAllPresenter.Callback,
    PurgeSinglePresenter.Callback,
    PurgePresenter.Callback {

  @field:Inject internal lateinit var presenter: PurgePresenter
  @field:Inject internal lateinit var purgeSinglePresenter: PurgeSinglePresenter
  @field:Inject internal lateinit var purgeAllPresenter: PurgeAllPresenter

  @field:Inject internal lateinit var purgeView: PurgeListView

  private lateinit var layoutRoot: ConstraintLayout

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = inflater.inflate(R.layout.layout_constraint, container, false)
    layoutRoot = root.findViewById(R.id.layout_constraint)

    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPurgeComponent()
        .toolbarActivity(requireToolbarActivity())
        .owner(viewLifecycleOwner)
        .parent(layoutRoot)
        .build()
        .inject(this)

    return root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    purgeView.inflate(savedInstanceState)
    layoutComponents(layoutRoot)

    presenter.bind(viewLifecycleOwner, this)
    purgeSinglePresenter.bind(viewLifecycleOwner, this)
    purgeAllPresenter.bind(viewLifecycleOwner, this)
  }

  private fun layoutComponents(layoutRoot: ConstraintLayout) {
    ConstraintSet().apply {
      clone(layoutRoot)

      purgeView.also {
        connect(it.id(), ConstraintSet.TOP, layoutRoot.id, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, layoutRoot.id, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, layoutRoot.id, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, layoutRoot.id, ConstraintSet.END)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      applyTo(layoutRoot)
    }
  }

  override fun onSinglePurged(stalePackage: String) {
    Timber.d("Purged stale: $stalePackage")
    presenter.fetchData(true)
  }

  override fun onAllPurged(stalePackages: List<String>) {
    Timber.d("Purged all stale: $stalePackages")
    presenter.fetchData(true)
  }

  override fun onDeleteAllRequest() {
    PurgeAllDialog.newInstance(purgeView.currentListData())
        .show(requireActivity(), "purge_all")
  }

  override fun onDeleteRequest(stalePackage: String) {
    PurgeSingleItemDialog.newInstance(stalePackage)
        .show(requireActivity(), "purge_single")
  }

  override fun onFetchStaleBegin() {
    purgeView.onStaleFetchBegin()
  }

  override fun onFetchStaleSuccess(data: List<String>) {
    purgeView.onStaleFetchSuccess(data)
  }

  override fun onFetchStaleError(throwable: Throwable) {
    purgeView.onStaleFetchError { presenter.fetchData(true) }
  }

  override fun onFetchStaleComplete() {
    purgeView.onStaleFetchComplete()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    purgeView.teardown()
  }

  override fun onStart() {
    super.onStart()
    presenter.fetchData(false)
  }

  override fun onPause() {
    super.onPause()
    if (this::purgeView.isInitialized) {
      purgeView.storeListPosition()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (this::purgeView.isInitialized) {
      purgeView.saveState(outState)
    }
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  companion object {

    const val TAG = "PurgeFragment"
  }
}
