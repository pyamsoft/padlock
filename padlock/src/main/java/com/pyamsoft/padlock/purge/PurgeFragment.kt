/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import com.pyamsoft.padlock.helper.refreshing
import com.pyamsoft.padlock.helper.retainAll
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.DialogUtil
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import timber.log.Timber
import javax.inject.Inject

class PurgeFragment : CanaryFragment(), PurgePresenter.View {
    @Inject internal lateinit var presenter: PurgePresenter
    private lateinit var adapter: ModelAdapter<String, PurgeItem>
    private lateinit var binding: FragmentPurgeBinding
    private lateinit var decoration: DividerItemDecoration
    private var lastPosition: Int = 0
    private val backingSet: MutableCollection<String> = LinkedHashSet()

    private fun decideListState() {
        binding.apply {
            if (adapter.adapterItemCount > 0) {
                purgeEmpty.visibility = View.GONE
                purgeList.visibility = View.VISIBLE
            } else {
                purgeList.visibility = View.GONE
                purgeEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
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
        backingSet.clear()

        toolbarActivity.withToolbar {
            it.menu.apply {
                removeGroup(R.id.menu_group_purge_all)
            }
            it.setOnMenuItemClickListener(null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                        DialogUtil.guaranteeSingleDialogFragment(activity, PurgeAllDialog(),
                                "purge_all")
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
            purgeSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
                    R.color.blue700, R.color.amber500)
            purgeSwipeRefresh.setOnRefreshListener { presenter.retrieveStaleApplications(true) }
        }
    }

    override fun onRetrieveBegin() {
        binding.purgeSwipeRefresh.refreshing(true)
        backingSet.clear()
    }

    override fun onRetrieveComplete() {
        adapter.retainAll(backingSet)
        lastPosition = ListStateUtil.restorePosition(lastPosition, binding.purgeList)
        decideListState()
        binding.purgeSwipeRefresh.refreshing(false)
    }

    override fun onRetrievedStale(packageName: String) {
        backingSet.add(packageName)

        var update = false
        for ((index, item) in adapter.adapterItems.withIndex()) {
            if (item.model == packageName) {
                update = true
                // Won't happen ever right now, keep in case the model gets more complex in future
                if (item.model != packageName) {
                    adapter.set(index, packageName)
                }
                break
            }
        }

        if (!update) {
            binding.apply {
                purgeEmpty.visibility = View.GONE
                purgeList.visibility = View.VISIBLE
            }

            var added = false
            for ((index, item) in adapter.adapterItems.withIndex()) {
                // The entry should go before this one
                if (packageName.compareTo(item.model, ignoreCase = true) < 0) {
                    added = true
                    adapter.add(index, packageName)
                    break
                }
            }

            if (!added) {
                // add at the end of the list
                adapter.add(packageName)
            }
        }
    }

    override fun onRetrieveError(throwable: Throwable) {
        Toasty.makeText(context!!, "Error retrieving old application list",
                Toasty.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        lastPosition = ListStateUtil.getCurrentPosition(binding.purgeList)
        ListStateUtil.saveState(TAG, null, binding.purgeList)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ListStateUtil.saveState(TAG, outState, binding.purgeList)
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

        adapter = ModelAdapter { PurgeItem(it) }
        binding.apply {
            purgeList.layoutManager = LinearLayoutManager(context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 3
            }
            purgeList.clipToPadding = false
            purgeList.setHasFixedSize(false)
            purgeList.addItemDecoration(decoration)
            purgeList.adapter = FastAdapter.with<PurgeItem, ModelAdapter<String, PurgeItem>>(
                    adapter)

            adapter.fastAdapter.apply {
                withSelectable(true)
                withOnClickListener { _, _, item, position ->
                    handleDeleteRequest(position, item.model)
                    return@withOnClickListener true
                }
            }

            purgeEmpty.visibility = View.GONE
            purgeList.visibility = View.VISIBLE
        }
    }

    private fun handleDeleteRequest(position: Int,
            packageName: String) {
        Timber.d("Handle delete request for %s at %d", packageName, position)
        DialogUtil.guaranteeSingleDialogFragment(activity,
                PurgeSingleItemDialog.newInstance(packageName), "purge_single")
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
