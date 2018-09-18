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

package com.pyamsoft.padlock.list

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.DISABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.ENABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PAUSED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PERMISSION
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.pin.PinDialog
import com.pyamsoft.padlock.service.ServiceManager
import com.pyamsoft.padlock.service.device.UsagePermissionChecker
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.fragment.toolbarActivity
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.hide
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.HideOnScrollListener
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

class LockListFragment : ToolbarFragment() {

  @field:Inject internal lateinit var imageLoader: ImageLoader
  @field:Inject internal lateinit var viewModel: LockListViewModel
  @field:Inject internal lateinit var serviceManager: ServiceManager

  private lateinit var binding: FragmentLockListBinding

  private var lastPosition: Int = 0
  private var displaySystemItem: MenuItem? = null

  private val handler by lazy(NONE) { Handler() }
  private val filterListDelegate by lazy(NONE) { FilterListDelegate() }
  private val adapter by lazy(NONE) {
    ModelAdapter<AppEntry, LockListItem> { LockListItem(requireActivity(), it) }
  }
  private val hideScrollListener by lazy(NONE) {
    // Attach the FAB to callbacks on the recycler scroll
    HideOnScrollListener.withView(binding.applistFab) {
      if (!binding.applistSwipeRefresh.isRefreshing) {
        if (it) {
          showFab()
        } else {
          hideFab()
        }
      }
    }
  }
  private val refreshLatch by lazy(NONE) {
    RefreshLatch.create(viewLifecycleOwner) {
      Timber.d("RefreshLatch refreshing $it")
      filterListDelegate.setEnabled(!it)
      binding.apply {
        applistSwipeRefresh.refreshing(it)

        if (it) {
          hideFab { hideScrollListener.syncVisibilityState() }
        } else {
          showFab { hideScrollListener.syncVisibilityState() }
        }
      }

      // Load is done
      if (!it) {
        if (adapter.adapterItemCount > 0) {
          showRecycler()
          Timber.d("We have refreshed")

          lastPosition = ListStateUtil.restorePosition(lastPosition, binding.applistRecyclerview)
        } else {
          binding.apply {
            applistRecyclerview.visibility = View.GONE
            applistEmpty.visibility = View.VISIBLE
          }
        }
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockListComponent(
            LockListProvider(viewLifecycleOwner, object : ListDiffProvider<AppEntry> {
              override fun data(): List<AppEntry> = Collections.unmodifiableList(adapter.models)
            })
        )
        .inject(this)
    binding = FragmentLockListBinding.inflate(inflater, container, false)

    filterListDelegate.onViewCreated(adapter)
    setupRecyclerView()
    setupSwipeRefresh()
    setupFAB()
    setupToolbarMenu()

    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)

    viewModel.onClearPinEvent {
      if (it.success) {
        onMasterPinClearSuccess()
      } else {
        onMasterPinClearFailure()
      }
      serviceManager.startService(false)
    }

    viewModel.onCreatePinEvent {
      if (it.success) {
        onMasterPinCreateSuccess()
      } else {
        onMasterPinCreateFailure()
      }
      serviceManager.startService(false)
    }

    viewModel.onModifyError { onModifyEntryError() }
    viewModel.onSystemVisibilityChanged { onSystemVisibilityChanged(it) }
    viewModel.onDatabaseChangeEvent { wrapper ->
      wrapper.onSuccess { onDatabaseChangeReceived(it.index, it.entry) }
      wrapper.onError { onDatabaseChangeError() }
    }
    viewModel.onFabStateChange { serviceState, fromClick ->
      when (serviceState) {
        ENABLED -> onFabIconLocked(fromClick)
        DISABLED -> onFabIconUnlocked(fromClick)
        PERMISSION -> onFabIconPermissionDenied(fromClick)
        PAUSED -> onFabIconPaused(fromClick)
      }
    }
    viewModel.onPopulateListEvent { wrapper ->
      wrapper.onLoading { onListPopulateBegin() }
      wrapper.onSuccess { onListLoaded(it) }
      wrapper.onError { onListPopulateError() }
      wrapper.onComplete { onListPopulated() }
    }

    val intent = requireActivity().intent
    if (intent.hasExtra(ServiceManager.FORCE_REFRESH_ON_OPEN)) {
      intent.removeExtra(ServiceManager.FORCE_REFRESH_ON_OPEN)

      Timber.d("Launched from notification, force list refresh")
      viewModel.populateList(true)
    }

    return binding.root
  }

  private fun setupToolbarMenu() {
    requireToolbarActivity().withToolbar {
      it.inflateMenu(R.menu.locklist_menu)
      it.inflateMenu(R.menu.search_menu)

      it.menu.apply {
        displaySystemItem = findItem(R.id.menu_is_system)
        filterListDelegate.onPrepareOptionsMenu(this, adapter)

        val searchItem = findItem(R.id.menu_search)
        val searchIcon = searchItem.icon
        searchIcon.mutate()
            .also { icon ->
              val tintedIcon = icon.tintWith(requireActivity(), R.color.black)
              searchItem.icon = tintedIcon
            }
      }

      it.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
          R.id.menu_is_system -> {
            if (!binding.applistSwipeRefresh.isRefreshing) {
              Timber.d("List is not refreshing. Allow change of system preference")
              viewModel.setSystemVisibility(!menuItem.isChecked)
              viewModel.populateList(true)
            }
            return@setOnMenuItemClickListener true
          }
          else -> {
            Timber.w("Unhandled menu item clicked: ${menuItem.itemId}")
            return@setOnMenuItemClickListener false
          }
        }
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    // Can sometimes be uninitialized
    if (this::binding.isInitialized) {
      ListStateUtil.saveState(TAG, outState, binding.applistRecyclerview)
    }
    super.onSaveInstanceState(outState)
  }

  override fun onStart() {
    super.onStart()
    handler.post { viewModel.populateList(false) }
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  override fun onPause() {
    super.onPause()
    lastPosition = ListStateUtil.getCurrentPosition(binding.applistRecyclerview)
    ListStateUtil.saveState(TAG, null, binding.applistRecyclerview)

    if (isRemoving) {
      toolbarActivity?.withToolbar {
        it.menu.apply {
          removeGroup(R.id.menu_group_list_system)
          removeGroup(R.id.menu_group_list_search)
        }
        it.setOnMenuItemClickListener(null)
      }
    }
  }

  override fun onStop() {
    super.onStop()
    handler.removeCallbacksAndMessages(null)
  }

  private inline fun showFab(crossinline onShown: FloatingActionButton.() -> Unit = {}) {
    binding.applistFab.show {
      onShown(this)
    }
  }

  private inline fun hideFab(crossinline onHidden: FloatingActionButton.() -> Unit = {}) {
    binding.applistFab.hide {
      onHidden(this)
    }
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.blue700)
      applistSwipeRefresh.setOnRefreshListener {
        Timber.d("onRefresh")
        refreshLatch.forceRefresh()
        viewModel.populateList(true)
      }
    }
  }

  private fun setupRecyclerView() {
    binding.applistRecyclerview.layoutManager = LinearLayoutManager(
        context
    )
        .apply {
          isItemPrefetchEnabled = true
          initialPrefetchItemCount = 3
        }

    binding.apply {
      applistRecyclerview.setHasFixedSize(true)
      applistRecyclerview.addItemDecoration(
          DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
      )
      applistRecyclerview.adapter =
          FastAdapter.with<LockListItem, ModelAdapter<AppEntry, LockListItem>>(
              adapter
          )

      adapter.fastAdapter.apply {
        withSelectable(true)
        withOnClickListener { _, _, item, _ ->
          displayLockInfoFragment(item.model)
          return@withOnClickListener true
        }
      }

      // First load should show the spinner
      applistEmpty.visibility = View.GONE
      applistRecyclerview.visibility = View.VISIBLE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    binding.apply {
      applistRecyclerview.setOnDebouncedClickListener(null)
      applistRecyclerview.removeOnScrollListener(hideScrollListener)
      applistRecyclerview.layoutManager = null
      applistRecyclerview.adapter = null
      applistFab.setOnDebouncedClickListener(null)
      applistSwipeRefresh.setOnRefreshListener(null)
      unbind()
    }
    adapter.clear()
  }

  private fun setupFAB() {
    binding.apply {
      applistFab.setOnDebouncedClickListener {
        viewModel.checkFabState(true)
      }
      applistRecyclerview.addOnScrollListener(hideScrollListener)
    }
  }

  private fun displayLockInfoFragment(entry: AppEntry) {
    LockInfoDialog.newInstance(entry)
        .show(requireActivity(), LockInfoDialog.TAG)
  }

  private fun onMasterPinCreateSuccess() {
    val v = view
    if (v != null) {
      Snackbreak.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  private fun onMasterPinCreateFailure() {
    Snackbreak.make(
        binding.root,
        "Failed to create master pin",
        Snackbar.LENGTH_SHORT
    )
        .show()
  }

  private fun onMasterPinClearSuccess() {
    val v = view
    if (v != null) {
      Snackbreak.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  private fun onMasterPinClearFailure() {
    Snackbreak.make(
        binding.root,
        "Failed to clear master pin",
        Snackbar.LENGTH_SHORT
    )
        .show()
  }

  private fun onModifyEntryError() {
    Snackbreak.make(
        binding.root,
        "Failed to modify application list",
        Snackbar.LENGTH_LONG
    )
        .setAction("Retry") { viewModel.populateList(true) }
        .show()
  }

  private fun onFabIconLocked(fromClick: Boolean) {
    Timber.d("on FAB enabled")

    imageLoader.load(R.drawable.ic_lock_outline_24dp)
        .into(binding.applistFab)
        .bind(viewLifecycleOwner)

    if (fromClick) {
      if (UsagePermissionChecker.hasPermission(requireContext())) {
        PinDialog.newInstance(checkOnly = false, finishOnDismiss = false)
            .show(requireActivity(), PinDialog.TAG)
      }
    }
  }

  private fun onFabIconUnlocked(fromClick: Boolean) {
    Timber.d("on FAB disabled")

    imageLoader.load(R.drawable.ic_lock_open_24dp)
        .into(binding.applistFab)
        .bind(viewLifecycleOwner)

    if (fromClick) {
      if (UsagePermissionChecker.hasPermission(requireContext())) {
        PinDialog.newInstance(checkOnly = false, finishOnDismiss = false)
            .show(requireActivity(), PinDialog.TAG)
      }
    }
  }

  private fun onFabIconPermissionDenied(fromClick: Boolean) {
    Timber.d("on FAB permission denied")

    imageLoader.load(R.drawable.ic_warning_24dp)
        .into(binding.applistFab)
        .bind(viewLifecycleOwner)

    if (fromClick) {
      UsageAccessRequestDialog().show(requireActivity(), "usage_access")
    }
  }

  private fun onFabIconPaused(fromClick: Boolean) {
    Timber.d("on FAB paused")

    imageLoader.load(R.drawable.ic_pause_24dp)
        .into(binding.applistFab)
        .bind(viewLifecycleOwner)

    if (fromClick) {
      serviceManager.startService(true)
    }
  }

  private fun onSystemVisibilityChanged(visible: Boolean) {
    displaySystemItem?.isChecked = visible
  }

  private fun onListPopulateBegin() {
    refreshLatch.isRefreshing = true
  }

  private fun onListLoaded(entries: List<AppEntry>) {
    adapter.set(entries)
  }

  private fun onListPopulated() {
    refreshLatch.isRefreshing = false
  }

  private fun onListPopulateError() {
    Snackbreak.make(
        binding.root,
        "Failed to load application list",
        Snackbar.LENGTH_LONG
    )
        .setAction("Retry") { viewModel.populateList(true) }
        .show()
  }

  private fun onDatabaseChangeError() {
    ErrorDialog().show(requireActivity(), "db_change_error")
  }

  private fun onDatabaseChangeReceived(
    index: Int,
    entry: AppEntry
  ) {
    adapter.set(index, entry)
  }

  private fun showRecycler() {
    binding.apply {
      applistRecyclerview.visibility = View.VISIBLE
      applistEmpty.visibility = View.GONE
      viewModel.checkFabState(false)
    }
  }

  companion object {

    const val TAG = "LockListFragment"
  }
}
