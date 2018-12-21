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

package com.pyamsoft.padlock.purge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.fragment.toolbarActivity
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class PurgeFragment : ToolbarFragment() {

  @field:Inject internal lateinit var viewModel: PurgeViewModel
  @field:Inject internal lateinit var purgeView: PurgeView

  private var fetchDisposable by singleDisposable()
  private var purgeAllDisposable by singleDisposable()
  private var purgeOneDisposable by singleDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPurgeComponent()
        .toolbarActivity(requireToolbarActivity())
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .diffProvider(object : ListDiffProvider<String> {
          override fun data(): List<String> = purgeView.getListModels()
        })
        .build()
        .inject(this)

    purgeView.create()
    return purgeView.root()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    purgeView.onSwipeToRefresh { fetchStalePackages(true) }
    purgeView.onListItemClicked { position: Int, model: String ->
      handleDeleteRequest(position, model)
    }
    purgeView.onToolbarMenuItemClicked { id: Int ->
      when (id) {
        R.id.menu_purge_all -> PurgeAllDialog().show(requireActivity(), "purge_all")
        else -> Timber.w("Unhandled menu item clicked: $id")
      }
    }

    purgeAllDisposable = viewModel.onPurgeAllEvent {
      Timber.d("Purged all stale: $it")
      fetchStalePackages(true)
    }

    purgeOneDisposable = viewModel.onPurgeEvent {
      Timber.d("Purged stale: $it")
      fetchStalePackages(true)
    }

  }

  private fun fetchStalePackages(force: Boolean) {
    fetchDisposable = viewModel.fetchStalePackages(
        force,
        onFetchBegin = { purgeView.onStaleFetchBegin(it) },
        onFetchSuccess = { purgeView.onStaleFetchSuccess(it) },
        onFetchError = { error: Throwable ->
          purgeView.onStaleFetchError(error) { fetchStalePackages(true) }
        },
        onFetchComplete = { purgeView.onStaleFetchComplete() }
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
    purgeOneDisposable.tryDispose()
    purgeAllDisposable.tryDispose()
    fetchDisposable.tryDispose()
  }

  override fun onStart() {
    super.onStart()
    fetchStalePackages(false)
  }

  override fun onPause() {
    super.onPause()
    if (this::purgeView.isInitialized) {
      purgeView.saveListPosition(null)
    }

    if (isRemoving) {
      toolbarActivity?.withToolbar {
        it.menu.apply {
          removeGroup(R.id.menu_group_purge_all)
        }
        it.setOnMenuItemClickListener(null)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (this::purgeView.isInitialized) {
      purgeView.saveListPosition(outState)
    }
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  private fun handleDeleteRequest(
    position: Int,
    packageName: String
  ) {
    Timber.d("Handle delete request for %s at %d", packageName, position)
    PurgeSingleItemDialog.newInstance(packageName)
        .show(requireActivity(), "purge_single")
  }

  companion object {

    const val TAG = "PurgeFragment"
  }
}
