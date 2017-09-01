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

package com.pyamsoft.padlock.purge

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentPurgeBinding
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class PurgeFragment : CanaryFragment(), PurgePresenter.BusCallback {
  @Inject internal lateinit var presenter: PurgePresenter
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var fastItemAdapter: FastItemAdapter<PurgeItem>
  private lateinit var binding: FragmentPurgeBinding
  private var decoration: DividerItemDecoration? = null

  private val onRetrieveBegin: () -> Unit = {
    binding.purgeEmpty.visibility = View.GONE
    binding.purgeList.visibility = View.GONE
  }

  private val onStaleApplicationReceived: (String) -> Unit = {
    fastItemAdapter.add(PurgeItem(it))
  }

  private val onRetrievalCompleted: () -> Unit = {
    handler.removeCallbacksAndMessages(null)
    handler.post {
      binding.purgeSwipeRefresh.post {
        if (binding.purgeSwipeRefresh != null) {
          binding.purgeSwipeRefresh.isRefreshing = false
        }
      }
    }

    if (fastItemAdapter.adapterItemCount > 0) {
      binding.purgeEmpty.visibility = View.GONE
      binding.purgeList.visibility = View.VISIBLE
    } else {
      binding.purgeList.visibility = View.GONE
      binding.purgeEmpty.visibility = View.VISIBLE
    }
  }


  override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    Injector.with(context) {
      it.inject(this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = FragmentPurgeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    handler.removeCallbacksAndMessages(null)
    fastItemAdapter.withOnClickListener(null)
    binding.purgeList.removeItemDecoration(decoration)
    binding.purgeList.setOnClickListener(null)
    binding.purgeList.layoutManager = null
    binding.purgeList.adapter = null
    binding.unbind()
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupRecyclerView()
    setupSwipeRefresh()

    presenter.bind(this)
  }

  private fun setupSwipeRefresh() {
    binding.purgeSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500)
    binding.purgeSwipeRefresh.setOnRefreshListener {
      Timber.d("onRefresh")
      refreshList()
    }
  }

  override fun onStart() {
    super.onStart()
    prepareRefresh()
    presenter.retrieveStaleApplications(false, onRetrieveBegin, onStaleApplicationReceived,
        onRetrievalCompleted)
  }

  private fun prepareRefresh() {
    fastItemAdapter.clear()
    handler.removeCallbacksAndMessages(null)
    handler.post {
      binding.purgeSwipeRefresh.post {
        if (binding.purgeSwipeRefresh != null) {
          binding.purgeSwipeRefresh.isRefreshing = true
        }
      }
    }
  }

  private fun refreshList() {
    prepareRefresh()
    presenter.retrieveStaleApplications(true, onRetrieveBegin, onStaleApplicationReceived,
        onRetrievalCompleted)
  }

  override fun onResume() {
    super.onResume()
    setActionBarUpEnabled(false)
    setActionBarTitle(R.string.app_name)
  }

  override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater?.inflate(R.menu.purge_old_menu, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val handled: Boolean = when (item.itemId) {
      R.id.menu_purge_all -> {
        DialogUtil.guaranteeSingleDialogFragment(activity, PurgeAllDialog(), "purge_all")
        true
      }
      else -> {
        Timber.d("Not handled: ${item.itemId}")
        false
      }
    }

    return handled || super.onOptionsItemSelected(item)
  }

  private fun setupRecyclerView() {
    fastItemAdapter = FastItemAdapter()
    fastItemAdapter.withSelectable(true)
    fastItemAdapter.withOnClickListener { _, _, item, position ->
      handleDeleteRequest(position, item.model)
      return@withOnClickListener true
    }
    decoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    val manager = LinearLayoutManager(context)
    manager.isItemPrefetchEnabled = true
    manager.initialPrefetchItemCount = 3
    binding.purgeList.layoutManager = manager
    binding.purgeList.clipToPadding = false
    binding.purgeList.setHasFixedSize(false)
    binding.purgeList.addItemDecoration(decoration)
    binding.purgeList.adapter = fastItemAdapter
  }

  private fun handleDeleteRequest(position: Int,
      packageName: String) {
    Timber.d("Handle delete request for %s at %d", packageName, position)
    DialogUtil.guaranteeSingleDialogFragment(activity,
        PurgeSingleItemDialog.newInstance(packageName), "purge_single")
  }

  override fun onPurge(packageName: String) {
    Timber.d("Purge stale: %s", packageName)
    presenter.deleteStale(packageName) { packageName1 ->
      val itemCount = fastItemAdapter.itemCount
      if (itemCount == 0) {
        Timber.e("Adapter is EMPTY")
      } else {
        var found = -1
        for (i in 0 until itemCount) {
          val item = fastItemAdapter.getAdapterItem(i)
          if (item.model == packageName1) {
            found = i
            break
          }
        }

        if (found != -1) {
          Timber.d("Remove deleted item: %s", packageName1)
          fastItemAdapter.remove(found)
        }
      }

      Unit
    }
  }

  override fun onPurgeAll() {
    val itemCount = fastItemAdapter.itemCount
    if (itemCount == 0) {
      Timber.e("Adapter is EMPTY")
    } else {
      for (item in fastItemAdapter.adapterItems) {
        onPurge(item.model)
      }
    }
  }

  companion object {

    const val TAG = "PurgeFragment"
  }
}
