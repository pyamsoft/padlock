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
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentPurgeBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.helper.tintIcon
import com.pyamsoft.pydroid.ui.app.activity.ToolbarActivity
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject

internal class PurgeViewImpl @Inject internal constructor(
  private val toolbarActivity: ToolbarActivity,
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?,
  private val theming: Theming
) : PurgeView, LifecycleObserver {

  private lateinit var adapter: ModelAdapter<String, PurgeItem>
  private lateinit var binding: FragmentPurgeBinding
  private lateinit var decoration: DividerItemDecoration

  private var lastPosition: Int = 0

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    binding.apply {
      purgeList.removeItemDecoration(decoration)
      purgeList.layoutManager = null
      purgeList.adapter = null
      unbind()
    }
    adapter.clear()
  }

  override fun create() {
    binding = FragmentPurgeBinding.inflate(inflater, container, false)
    setupRecyclerView()
    setupSwipeRefresh()
    setupToolbarMenu()
    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)
  }

  override fun root(): View {
    return binding.root
  }

  override fun getListModels(): List<String> {
    return adapter.models.toList()
  }

  override fun onSwipeToRefresh(onSwipe: () -> Unit) {
    binding.purgeSwipeRefresh.setOnRefreshListener {
      startRefreshing()
      onSwipe()
    }
  }

  override fun onListItemClicked(onClick: (position: Int, model: String) -> Unit) {
    adapter.fastAdapter.withOnClickListener { _, _, item, position ->
      onClick(position, item.model)
      return@withOnClickListener true
    }
  }

  override fun onToolbarMenuItemClicked(onClick: (id: Int) -> Unit) {
    toolbarActivity.requireToolbar { toolbar ->
      toolbar.setOnMenuItemClickListener {
        onClick(it.itemId)
        return@setOnMenuItemClickListener true
      }
    }
  }

  override fun onStaleFetchBegin(forced: Boolean) {
    startRefreshing()
  }

  override fun onStaleFetchSuccess(stalePackages: List<String>) {
    adapter.set(stalePackages)
  }

  override fun onStaleFetchError(
    error: Throwable,
    onRetry: () -> Unit
  ) {
    Snackbreak.bindTo(owner)
        .long(binding.root, "Failed to load outdated application list")
        .setAction("Retry") { onRetry() }
        .show()
  }

  override fun onStaleFetchComplete() {
    doneRefreshing()
  }

  override fun saveListPosition(outState: Bundle?) {
    if (this::binding.isInitialized) {
      if (outState == null) {
        lastPosition = ListStateUtil.getCurrentPosition(binding.purgeList)
        ListStateUtil.saveState(TAG, null, binding.purgeList)
      } else {
        ListStateUtil.saveState(TAG, outState, binding.purgeList)
      }
    }
  }

  private fun setupSwipeRefresh() {
    binding.purgeSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.blue700)
  }

  private fun startRefreshing() {
    binding.purgeSwipeRefresh.refreshing(true)
  }

  private fun doneRefreshing() {
    binding.purgeSwipeRefresh.refreshing(false)
    lastPosition = ListStateUtil.restorePosition(lastPosition, binding.purgeList)
    decideListState()
  }

  private fun setupRecyclerView() {
    val context = root().context
    decoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)

    adapter = ModelAdapter { PurgeItem(it) }
    binding.apply {
      purgeList.layoutManager = LinearLayoutManager(context).apply {
        isItemPrefetchEnabled = true
        initialPrefetchItemCount = 3
      }
      purgeList.setHasFixedSize(true)
      purgeList.addItemDecoration(decoration)
      purgeList.adapter = FastAdapter.with<PurgeItem, ModelAdapter<String, PurgeItem>>(adapter)
      adapter.fastAdapter.withSelectable(true)
    }

    // Set up initial state
    showRecycler()
  }

  private fun setupToolbarMenu() {
    toolbarActivity.requireToolbar { toolbar ->
      toolbar.inflateMenu(R.menu.purge_old_menu)
      toolbar.menu.tintIcon(toolbar.context, theming, R.id.menu_purge_all)
    }
  }

  private fun showRecycler() {
    binding.apply {
      purgeEmpty.visibility = View.GONE
      purgeList.visibility = View.VISIBLE
    }
  }

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

  companion object {

    private const val TAG = "PurgeViewImpl"
  }
}