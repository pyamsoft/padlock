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
import android.support.design.widget.Snackbar
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.commons.utils.FastAdapterDiffUtil
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.helper.NeverNotifyItemList
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.service.device.UsagePermissionChecker
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.design.fab.HideScrollFABBehavior
import com.pyamsoft.pydroid.design.util.refreshing
import com.pyamsoft.pydroid.design.util.withBehavior
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.util.popHide
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import com.pyamsoft.pydroid.util.Toasty
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

class LockListFragment : CanaryFragment(), LockListPresenter.View {

  @field:Inject
  internal lateinit var imageLoader: ImageLoader
  @field:Inject
  internal lateinit var presenter: LockListPresenter
  private lateinit var adapter: ModelAdapter<AppEntry, LockListItem>
  private lateinit var binding: FragmentLockListBinding
  private lateinit var filterListDelegate: FilterListDelegate
  private lateinit var refreshLatch: RefreshLatch
  private var dividerDecoration: DividerItemDecoration? = null
  private var lastPosition: Int = 0
  private var displaySystemItem: MenuItem? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockListComponent(LockListProvider(object : ListDiffProvider<AppEntry> {
          override fun data(): List<AppEntry> = Collections.unmodifiableList(adapter.models)
        }))
        .inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentLockListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    refreshLatch = RefreshLatch.create(viewLifecycle) {
      activity?.invalidateOptionsMenu()
      filterListDelegate.setEnabled(!it)
      binding.apply {
        applistSwipeRefresh.refreshing(it)

        if (it) {
          applistFab.hide()
        } else {
          applistFab.show()
        }
      }

      // Load is done
      if (!it) {
        if (adapter.adapterItemCount > 0) {
          showRecycler()
          Timber.d("We have refreshed")
          presenter.showOnBoarding()

          lastPosition = ListStateUtil.restorePosition(lastPosition, binding.applistRecyclerview)
        } else {
          binding.apply {
            applistRecyclerview.visibility = View.GONE
            applistEmpty.visibility = View.VISIBLE
          }
          Toasty.makeText(
              binding.applistRecyclerview.context,
              "Error while loading list. Please try again.",
              Toast.LENGTH_SHORT
          )
        }
      }
    }
    adapter = ModelAdapter(NeverNotifyItemList.create()) { LockListItem(requireActivity(), it) }
    filterListDelegate = FilterListDelegate()
    filterListDelegate.onViewCreated(adapter)
    setupRecyclerView()
    setupSwipeRefresh()
    setupFAB()
    setupToolbarMenu()

    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)

    presenter.bind(viewLifecycle, this)

    val intent = requireActivity().intent
    if (intent.hasExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST)) {
      intent.removeExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST)

      Timber.d("Launched from notification, clear list")
      presenter.forceClearCache()
    }
  }

  private fun setupToolbarMenu() {
    toolbarActivity.withToolbar {
      it.inflateMenu(R.menu.locklist_menu)
      it.inflateMenu(R.menu.search_menu)

      it.menu.apply {
        setupDisplaySystemVisibleItem(this)
        filterListDelegate.onPrepareOptionsMenu(this, adapter)
      }

      it.setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_is_system -> {
            if (!binding.applistSwipeRefresh.isRefreshing) {
              Timber.d("List is not refreshing. Allow change of system preference")
              presenter.setSystemVisibility(!it.isChecked)
              presenter.populateList(true)
            }
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

  override fun onSaveInstanceState(outState: Bundle) {
    // Can sometimes be uninitialized
    if (this::binding.isInitialized) {
      ListStateUtil.saveState(TAG, outState, binding.applistRecyclerview)
    }
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    toolbarActivity.withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }

    binding.applistFab.popShow()
  }

  override fun onPause() {
    super.onPause()
    binding.applistFab.popHide()
    lastPosition = ListStateUtil.getCurrentPosition(binding.applistRecyclerview)
    ListStateUtil.saveState(TAG, null, binding.applistRecyclerview)
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      applistSwipeRefresh.setColorSchemeResources(
          R.color.blue500, R.color.amber700,
          R.color.blue700, R.color.amber500
      )
      applistSwipeRefresh.setOnRefreshListener {
        Timber.d("onRefresh")
        refreshLatch.forceRefresh()
        presenter.populateList(true)
      }
    }
  }

  private fun setupRecyclerView() {
    dividerDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    binding.applistRecyclerview.layoutManager = LinearLayoutManager(context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    binding.apply {
      applistRecyclerview.setHasFixedSize(true)
      applistRecyclerview.addItemDecoration(dividerDecoration)
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

  private fun setupDisplaySystemVisibleItem(menu: Menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system)
    presenter.setSystemVisibilityFromPreference()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    binding.apply {
      applistRecyclerview.removeItemDecoration(dividerDecoration)
      applistRecyclerview.setOnDebouncedClickListener(null)
      applistRecyclerview.layoutManager = null
      applistRecyclerview.adapter = null
      applistFab.setOnDebouncedClickListener(null)
      applistSwipeRefresh.setOnRefreshListener(null)
      unbind()
    }
    adapter.clear()

    toolbarActivity.withToolbar {
      it.menu.apply {
        removeGroup(R.id.menu_group_list_system)
        removeGroup(R.id.menu_group_list_search)
      }
      it.setOnMenuItemClickListener(null)
    }
  }

  private fun setupFAB() {
    binding.apply {
      applistFab.setOnDebouncedClickListener {
        if (UsagePermissionChecker.missingUsageStatsPermission(
                applistFab.context
            )
        ) {
          UsageAccessRequestDialog().show(requireActivity(), "usage_access")
        } else {
          requireActivity().let {
            PinEntryDialog.newInstance(it.packageName)
                .show(it, PinEntryDialog.TAG)
          }
        }
      }
      applistFab.withBehavior(HideScrollFABBehavior(24))
    }
  }

  private fun displayLockInfoFragment(entry: AppEntry) {
    LockInfoDialog.newInstance(entry)
        .show(requireActivity(), LockInfoDialog.TAG)
  }

  override fun onMasterPinCreateSuccess() {
    onFABEnabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  override fun onMasterPinCreateFailure() {
    Toasty.makeText(requireContext(), "Error: Mismatched PIN", Toast.LENGTH_SHORT)
  }

  override fun onMasterPinClearSuccess() {
    onFABDisabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  override fun onMasterPinClearFailure() {
    Toasty.makeText(requireContext(), "Error: Invalid PIN", Toast.LENGTH_SHORT)
  }

  private fun refreshListEntry(
    packageName: String,
    locked: Boolean? = null,
    whitelisted: Boolean? = null,
    hardlocked: Boolean? = null
  ) {
    for (i in adapter.adapterItems.indices) {
      val item: LockListItem = adapter.getAdapterItem(i)
      val entry: AppEntry = item.model
      if (packageName == entry.packageName) {
        val newLocked: Boolean = locked ?: entry.locked
        val newWhitelisted: Int = maxOf(
            0, when {
          whitelisted == null -> entry.whitelisted
          whitelisted -> entry.whitelisted + 1
          else -> entry.whitelisted - 1
        }
        )
        val newHardLocked: Int = maxOf(
            0, when {
          hardlocked == null -> entry.hardLocked
          hardlocked -> entry.hardLocked + 1
          else -> entry.hardLocked - 1
        }
        )

        adapter.set(
            i, AppEntry(
            name = entry.name, packageName = entry.packageName,
            system = entry.system,
            locked = newLocked, whitelisted = newWhitelisted,
            hardLocked = newHardLocked
        )
        )
        break
      }
    }
  }

  override fun onModifyEntryCreated(packageName: String) {
    Timber.d("Created entry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = true, whitelisted = null,
        hardlocked = null
    )
  }

  override fun onModifyEntryDeleted(packageName: String) {
    Timber.d("Deleted entry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = false, whitelisted = null,
        hardlocked = null
    )
  }

  override fun onModifyEntryError(throwable: Throwable) {
    ErrorDialog().show(requireActivity(), "list_error")
  }

  override fun onModifySubEntryToDefaultFromWhitelisted(packageName: String) {
    Timber.d("Defaulted from whitelist subentry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = null, whitelisted = false,
        hardlocked = null
    )
  }

  override fun onModifySubEntryToDefaultFromHardlocked(packageName: String) {
    Timber.d("Defaulted from hardlock subentry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = null, whitelisted = null,
        hardlocked = false
    )
  }

  override fun onModifySubEntryToWhitelistedFromDefault(packageName: String) {
    Timber.d("Whitelisted from default subentry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = null, whitelisted = true,
        hardlocked = null
    )
  }

  override fun onModifySubEntryToWhitelistedFromHardlocked(packageName: String) {
    Timber.d("Whitelisted from hardlock subentry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = null, whitelisted = true,
        hardlocked = false
    )
  }

  override fun onModifySubEntryToHardlockedFromDefault(packageName: String) {
    Timber.d("Hardlocked from default subentry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = null, whitelisted = null,
        hardlocked = true
    )
  }

  override fun onModifySubEntryToHardlockedFromWhitelisted(packageName: String) {
    Timber.d("Hardlocked from whitelisted subentry for $packageName")
    refreshListEntry(
        packageName = packageName, locked = null, whitelisted = false,
        hardlocked = true
    )
  }

  override fun onModifySubEntryError(throwable: Throwable) {
    ErrorDialog().show(requireActivity(), "list_error")
  }

  override fun onFABEnabled() {
    imageLoader.fromResource(R.drawable.ic_lock_outline_24dp)
        .into(binding.applistFab)
        .bind(viewLifecycle)
  }

  override fun onFABDisabled() {
    imageLoader.fromResource(R.drawable.ic_lock_open_24dp)
        .into(binding.applistFab)
        .bind(viewLifecycle)
  }

  override fun onSystemVisibilityChanged(visible: Boolean) {
    displaySystemItem?.isChecked = visible
  }

  override fun onOnboardingComplete() {
    Timber.d("Onboarding complete")
  }

  override fun onShowOnboarding() {
    Timber.d("Show onboarding")
  }

  override fun onListPopulateBegin() {
    refreshLatch.isRefreshing = true
  }

  override fun onListLoaded(result: ListDiffResult<AppEntry>) {
    result.ifEmpty { adapter.clear() }
    result.withValues {
      adapter.setNewList(it.list())
      it.dispatch { FastAdapterDiffUtil.set(adapter, it) }
    }
  }

  private fun showRecycler() {
    binding.apply {
      applistRecyclerview.visibility = View.VISIBLE
      applistEmpty.visibility = View.GONE
    }
  }

  override fun onListPopulated() {
    refreshLatch.isRefreshing = false
  }

  override fun onListPopulateError(throwable: Throwable) {
    ErrorDialog().show(requireActivity(), "list_error")
  }

  companion object {

    const val TAG = "LockListFragment"
  }
}
