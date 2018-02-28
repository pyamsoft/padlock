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
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
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
import com.pyamsoft.padlock.helper.NeverNotifyItemList
import com.pyamsoft.padlock.helper.dispatch
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.design.util.refreshing
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import com.pyamsoft.pydroid.util.Toasty
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

class PurgeFragment : CanaryFragment(), PurgePresenter.View {
  @Inject
  internal lateinit var presenter: PurgePresenter
  private lateinit var adapter: ModelAdapter<String, PurgeItem>
  private lateinit var binding: FragmentPurgeBinding
  private lateinit var decoration: DividerItemDecoration
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPurgeComponent(PurgeProvider(object : ListDiffProvider<List<String>> {
          override fun data(): List<String> = Collections.unmodifiableList(adapter.models)
        }))
        .inject(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = FragmentPurgeBinding.inflate(inflater, container, false)
    return binding.root
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

    toolbarActivity.withToolbar {
      it.menu.apply {
        removeGroup(R.id.menu_group_purge_all)
      }
      it.setOnMenuItemClickListener(null)
    }
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    refreshLatch = RefreshLatch.create(viewLifecycle) {
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

    presenter.bind(viewLifecycle, this)
  }

  private fun setupToolbarMenu() {
    toolbarActivity.withToolbar {
      it.inflateMenu(R.menu.purge_old_menu)

      it.setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_purge_all -> {
            PurgeAllDialog().show(activity, "purge_all")
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
      purgeSwipeRefresh.setColorSchemeResources(
          R.color.blue500, R.color.amber700,
          R.color.blue700, R.color.amber500
      )
      purgeSwipeRefresh.setOnRefreshListener {
        refreshLatch.forceRefresh()
        presenter.retrieveStaleApplications(true)
      }
    }
  }

  override fun onRetrieveBegin() {
    refreshLatch.isRefreshing = true
  }

  override fun onRetrieveComplete() {
    refreshLatch.isRefreshing = false
  }

  override fun onRetrievedList(result: ListDiffResult<String>) {
    result.ifEmpty { adapter.clear() }
    result.withValues {
      adapter.setNewList(it.list())
      it.dispatch(adapter)
    }
  }

  override fun onRetrieveError(throwable: Throwable) {
    Toasty.makeText(
        requireContext(), "Error retrieving old application list",
        Toasty.LENGTH_SHORT
    )
  }

  override fun onPause() {
    super.onPause()
    lastPosition = ListStateUtil.getCurrentPosition(binding.purgeList)
    ListStateUtil.saveState(TAG, null, binding.purgeList)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (this::binding.isInitialized) {
      ListStateUtil.saveState(TAG, outState, binding.purgeList)
    }
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    toolbarActivity.withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  private fun setupRecyclerView() {
    decoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)

    adapter = ModelAdapter(NeverNotifyItemList.create()) { PurgeItem(it) }
    binding.apply {
      purgeList.layoutManager = LinearLayoutManager(context).apply {
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
        .show(activity, "purge_single")
  }

  override fun onPurge(packageName: String) {
    Timber.d("Purge stale: %s", packageName)
    presenter.deleteStale(packageName)
  }

  override fun onDeleted(packageName: String) {
    val itemCount = adapter.fastAdapter.itemCount
    if (itemCount == 0) {
      Timber.e("Adapter is EMPTY")
    } else {
      var found = -1
      for (i in 0 until itemCount) {
        val item = adapter.getAdapterItem(i)
        if (item.model == packageName) {
          found = i
          break
        }
      }

      if (found != -1) {
        Timber.d("Remove deleted item: %s", packageName)
        adapter.remove(found)
      }
    }

    decideListState()
  }

  override fun onPurgeAll() {
    val itemCount = adapter.fastAdapter.itemCount
    if (itemCount == 0) {
      Timber.e("Adapter is EMPTY")
    } else {
      for (item in adapter.adapterItems) {
        onPurge(item.model)
      }
    }

    decideListState()
  }

  companion object {

    const val TAG = "PurgeFragment"
  }
}
