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

package com.pyamsoft.padlock.list

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.Toast
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.helper.refreshing
import com.pyamsoft.padlock.helper.retainAll
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.service.UsagePermissionChecker
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.design.fab.HideScrollFABBehavior
import com.pyamsoft.pydroid.design.util.FABUtil
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.helper.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.AnimUtil
import com.pyamsoft.pydroid.ui.util.DialogUtil
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import timber.log.Timber
import javax.inject.Inject

class LockListFragment : CanaryFragment(), LockListPresenter.View {

  @field:Inject
  internal lateinit var imageLoader: ImageLoader
  @field:Inject
  internal lateinit var presenter: LockListPresenter
  private lateinit var adapter: ModelAdapter<AppEntry, LockListItem>
  private lateinit var binding: FragmentLockListBinding
  private lateinit var filterListDelegate: FilterListDelegate
  private var dividerDecoration: DividerItemDecoration? = null
  private var lastPosition: Int = 0
  private var displaySystemItem: MenuItem? = null
  private val backingSet: MutableCollection<AppEntry> = LinkedHashSet()
  private lateinit var refreshLatch: RefreshLatch

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(context!!.applicationContext)
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
        adapter.retainAll(backingSet)
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
              .show()
        }
      }
    }
    adapter = ModelAdapter { LockListItem(activity!!, it) }
    filterListDelegate = FilterListDelegate()
    filterListDelegate.onViewCreated(adapter)
    setupRecyclerView()
    setupSwipeRefresh()
    setupFAB()
    setupToolbarMenu()

    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)

    presenter.bind(viewLifecycle, this)

    val intent = activity!!.intent
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

    AnimUtil.popShow(binding.applistFab, 300, 400)
  }

  override fun onPause() {
    super.onPause()
    AnimUtil.popHide(binding.applistFab, 300, 400)
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
    backingSet.clear()

    toolbarActivity.withToolbar {
      it.menu.apply {
        removeGroup(R.id.menu_group_list_system)
        removeGroup(R.id.menu_group_list_search)
      }
      it.setOnMenuItemClickListener(null)
    }
  }

  private fun setupFAB() {
    binding.applistFab.setOnDebouncedClickListener {
      if (UsagePermissionChecker.missingUsageStatsPermission(
              binding.applistFab.context
          )) {
        DialogUtil.guaranteeSingleDialogFragment(
            activity, UsageAccessRequestDialog(),
            "accessibility"
        )
      } else {
        DialogUtil.guaranteeSingleDialogFragment(
            activity,
            PinEntryDialog.newInstance(binding.applistFab.context.packageName),
            PinEntryDialog.TAG
        )
      }
    }
    FABUtil.setupFABBehavior(binding.applistFab, HideScrollFABBehavior(24))
  }

  private fun displayLockInfoFragment(entry: AppEntry) {
    DialogUtil.guaranteeSingleDialogFragment(
        activity, LockInfoDialog.newInstance(entry),
        LockInfoDialog.TAG
    )
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
    Toasty.makeText(context!!, "Error: Mismatched PIN", Toast.LENGTH_SHORT)
        .show()
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
    Toasty.makeText(context!!, "Error: Invalid PIN", Toast.LENGTH_SHORT)
        .show()
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

        // Update cache with the whitelist numbers so that a soft refresh will not change visual
        presenter.updateCache(packageName, newWhitelisted, newHardLocked)
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
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
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
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
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
    refreshLatch.refreshing = true
    backingSet.clear()
  }

  override fun onEntryAddedToList(entry: AppEntry) {
    backingSet.add(entry)

    var update = false
    for ((index, item) in adapter.adapterItems.withIndex()) {
      if (item.model.packageName == entry.packageName) {
        update = true
        if (item.model != entry) {
          adapter.set(index, entry)
        }
        break
      }
    }

    if (!update) {
      showRecycler()

      var added = false
      for ((index, item) in adapter.adapterItems.withIndex()) {
        // The entry should go before this one
        if (entry.name.compareTo(item.model.name, ignoreCase = true) < 0) {
          added = true
          adapter.add(index, entry)
          break
        }
      }

      if (!added) {
        // add at the end of the list
        adapter.add(entry)
      }
    }
  }

  private fun showRecycler() {
    binding.apply {
      applistRecyclerview.visibility = View.VISIBLE
      applistEmpty.visibility = View.GONE
    }
  }

  override fun onListPopulated() {
    refreshLatch.refreshing = false
  }

  override fun onListPopulateError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }

  companion object {

    const val TAG = "LockListFragment"
  }
}
