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
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentPurgeBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.fragment.toolbarActivity
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import timber.log.Timber
import javax.inject.Inject

class PurgeFragment : ToolbarFragment() {

  @field:Inject
  internal lateinit var viewModel: PurgeViewModel

  private lateinit var adapter: ModelAdapter<String, PurgeItem>
  private lateinit var binding: FragmentPurgeBinding
  private lateinit var decoration: androidx.recyclerview.widget.DividerItemDecoration
  private lateinit var refreshLatch: RefreshLatch
  private var lastPosition: Int = 0

  private fun decideListState() {
    binding.apply {
      if (adapter.adapterItemCount > 0) {
        showRecycler()
      } else {
        purgeList.visibility = View.GONE
        purgeEmpty.visibility = View.VISIBLE
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPurgeComponent(PurgeModule(viewLifecycleOwner, object : ListDiffProvider<String> {
          override fun data(): List<String> {
            return adapter.models.toList()
          }
        }))
        .inject(this)
    binding = FragmentPurgeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    refreshLatch = RefreshLatch.create(viewLifecycleOwner) {
      binding.purgeSwipeRefresh.refreshing(it)

      // Load complete
      if (!it) {
        lastPosition = ListStateUtil.restorePosition(lastPosition, binding.purgeList)
        decideListState()
      }
    }
    setupRecyclerView()
    setupSwipeRefresh()
    setupToolbarMenu()
    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)

    viewModel.onPurgeAllEvent {
      Timber.d("Purged stale: $it")
      viewModel.fetch(true)
    }

    viewModel.onPurgeEvent {
      Timber.d("Purged stale: $it")
      viewModel.fetch(true)
    }

    viewModel.onStaleApplicationsFetched { wrapper ->
      wrapper.onLoading { refreshLatch.isRefreshing = true }
      wrapper.onComplete { refreshLatch.isRefreshing = false }
      wrapper.onError { _ ->
        Snackbreak.long(binding.root, "Failed to load outdated application list")
            .setAction("Retry") { viewModel.fetch(true) }
            .show()
      }
      wrapper.onSuccess { adapter.set(it) }
    }

  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.apply {
      purgeList.removeItemDecoration(decoration)
      purgeList.layoutManager = null
      purgeList.adapter = null
      unbind()
    }
    adapter.clear()
  }

  override fun onStart() {
    super.onStart()
    viewModel.fetch(false)
  }

  private fun setupToolbarMenu() {
    requireToolbarActivity().withToolbar { toolbar ->
      toolbar.inflateMenu(R.menu.purge_old_menu)

      toolbar.setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_purge_all -> {
            PurgeAllDialog().show(requireActivity(), "purge_all")
            return@setOnMenuItemClickListener true
          }
          else -> {
            Timber.w("Unhandled menu item clicked: ${it.itemId}")
            return@setOnMenuItemClickListener false
          }
        }
      }
    }
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      purgeSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.blue700)
      purgeSwipeRefresh.setOnRefreshListener {
        refreshLatch.forceRefresh()
        viewModel.fetch(true)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    lastPosition = ListStateUtil.getCurrentPosition(binding.purgeList)
    ListStateUtil.saveState(TAG, null, binding.purgeList)

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
    if (this::binding.isInitialized) {
      ListStateUtil.saveState(TAG, outState, binding.purgeList)
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

  private fun setupRecyclerView() {
    decoration = androidx.recyclerview.widget.DividerItemDecoration(
        context, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
    )

    adapter = ModelAdapter { PurgeItem(it) }
    binding.apply {
      purgeList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
          .apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = 3
          }
      purgeList.setHasFixedSize(true)
      purgeList.addItemDecoration(decoration)
      purgeList.adapter = FastAdapter.with<PurgeItem, ModelAdapter<String, PurgeItem>>(
          adapter
      )

      adapter.fastAdapter.apply {
        withSelectable(true)
        withOnClickListener { _, _, item, position ->
          handleDeleteRequest(position, item.model)
          return@withOnClickListener true
        }
      }

      // Set up initial state
      showRecycler()
    }
  }

  private fun showRecycler() {
    binding.apply {
      purgeEmpty.visibility = View.GONE
      purgeList.visibility = View.VISIBLE
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
