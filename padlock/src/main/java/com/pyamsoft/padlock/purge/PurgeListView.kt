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
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.purge.PurgeListView.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject

internal class PurgeListView @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  private lateinit var modelAdapter: ModelAdapter<String, PurgeItem>
  private var decoration: DividerItemDecoration? = null

  private var lastPosition: Int = 0

  private val emptyState by lazyView<TextView>(R.id.swipe_refresh_empty)
  private val listView by lazyView<RecyclerView>(R.id.swipe_refresh_list)

  override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.swipe_refresh)

  override val layout: Int = R.layout.layout_swipe_refresh

  override fun onTeardown() {
    listView.apply {
      decoration?.also { removeItemDecoration(it) }
      decoration = null

      layoutManager = null
      adapter = null
    }

    modelAdapter.clear()

    layoutRoot.setOnRefreshListener(null)
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
    setupRecyclerView()
    setupSwipeRefresh()
    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)
  }

  fun currentListData(): List<String> {
    return modelAdapter.models.toList()
  }

  fun onStaleFetchBegin() {
    startRefreshing()
  }

  fun onStaleFetchSuccess(stalePackages: List<String>) {
    modelAdapter.set(stalePackages)
  }

  fun onStaleFetchError(onRetry: () -> Unit) {
    Snackbreak.bindTo(owner)
        .long(layoutRoot, "Failed to load outdated application list")
        .setAction("Retry") { onRetry() }
        .show()
  }

  fun showErrorMessage(message: String) {
    Snackbreak.bindTo(owner)
        .long(layoutRoot, message)
        .show()
  }

  fun onStaleFetchComplete() {
    doneRefreshing()
  }

  override fun onSaveState(outState: Bundle) {
    saveListPosition(outState)
  }

  fun storeListPosition() {
    saveListPosition(null)
  }

  private fun saveListPosition(outState: Bundle?) {
    if (outState == null) {
      lastPosition = ListStateUtil.getCurrentPosition(listView)
      ListStateUtil.saveState(TAG, null, listView)
    } else {
      ListStateUtil.saveState(TAG, outState, listView)
    }
  }

  private fun setupSwipeRefresh() {
    layoutRoot.apply {
      setColorSchemeResources(R.color.blue500, R.color.blue700)

      setOnRefreshListener {
        callback.onRefresh(true)
      }
    }
  }

  private fun startRefreshing() {
    layoutRoot.refreshing(true)
  }

  private fun doneRefreshing() {
    layoutRoot.refreshing(false)
    lastPosition = ListStateUtil.restorePosition(lastPosition, listView)
    decideListState()
  }

  private fun setupRecyclerView() {
    val context = layoutRoot.context
    val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    decoration?.also { listView.removeItemDecoration(it) }
    decoration = itemDecoration

    modelAdapter = ModelAdapter { PurgeItem(it) }

    listView.apply {
      layoutManager = LinearLayoutManager(context).apply {
        isItemPrefetchEnabled = true
        initialPrefetchItemCount = 3
      }
      setHasFixedSize(true)
      addItemDecoration(itemDecoration)
      adapter = FastAdapter.with<PurgeItem, ModelAdapter<String, PurgeItem>>(modelAdapter)
    }

    modelAdapter.fastAdapter.apply {
      withSelectable(true)
      withOnClickListener { _, _, item, _ ->
        callback.onListItemClicked(item.model)
        return@withOnClickListener true
      }
    }

    // Set up initial state
    showRecycler()
  }

  private fun showRecycler() {
    emptyState.visibility = View.GONE
    listView.visibility = View.VISIBLE
  }

  private fun hideRecycler() {
    listView.visibility = View.GONE
    emptyState.setText(R.string.purge_all_clean)
    emptyState.visibility = View.VISIBLE
  }

  private fun decideListState() {
    if (modelAdapter.adapterItemCount > 0) {
      showRecycler()
    } else {
      hideRecycler()
    }
  }

  interface Callback {

    fun onRefresh(forced: Boolean)

    fun onListItemClicked(stalePackage: String)

  }

  companion object {

    private const val TAG = "PurgeListView"
  }
}