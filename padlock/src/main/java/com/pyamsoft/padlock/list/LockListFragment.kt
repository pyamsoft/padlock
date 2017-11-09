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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
import com.pyamsoft.padlock.helper.refreshing
import com.pyamsoft.padlock.helper.retainAll
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.padlock.uicommon.ListStateUtil
import com.pyamsoft.pydroid.design.fab.HideScrollFABBehavior
import com.pyamsoft.pydroid.design.util.FABUtil
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.helper.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.AnimUtil
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockListFragment : CanaryFragment(), LockListPresenter.View {

  @field:Inject internal lateinit var imageLoader: ImageLoader
  @field:Inject internal lateinit var presenter: LockListPresenter
  private lateinit var adapter: ModelAdapter<AppEntry, LockListItem>
  private lateinit var binding: FragmentLockListBinding
  private lateinit var filterListDelegate: FilterListDelegate
  private var fabIconTask = LoaderHelper.empty()
  private var dividerDecoration: DividerItemDecoration? = null
  private var lastPosition: Int = 0
  private var displaySystemItem: MenuItem? = null
  private val backingSet: MutableCollection<AppEntry> = LinkedHashSet()

  override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    filterListDelegate = FilterListDelegate()
    adapter = ModelAdapter { LockListItem(activity!!, it) }
    binding = FragmentLockListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    filterListDelegate.onViewCreated(adapter)
    setupRecyclerView()
    setupSwipeRefresh()
    setupFAB()

    lastPosition = ListStateUtil.restoreState(savedInstanceState)

    presenter.bind(this)

    val intent = activity!!.intent
    if (intent.hasExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST)) {
      intent.removeExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST)

      Timber.d("Launched from notification, clear list")
      presenter.forceClearCache()
    }
  }

  override fun onStart() {
    super.onStart()
    presenter.populateList(false)
    presenter.setFABStateFromPreference()
  }

  override fun onStop() {
    super.onStop()
    lastPosition = ListStateUtil.getCurrentPosition(binding.applistRecyclerview)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    ListStateUtil.saveState(outState, binding.applistRecyclerview)
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    setActionBarUpEnabled(false)
    setActionBarTitle(R.string.app_name)
    AnimUtil.popShow(binding.applistFab, 300, 400)
  }

  override fun onPause() {
    super.onPause()
    AnimUtil.popHide(binding.applistFab, 300, 400)
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
          R.color.blue700, R.color.amber500)
      applistSwipeRefresh.setOnRefreshListener {
        Timber.d("onRefresh")
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
      applistRecyclerview.clipToPadding = false
      applistRecyclerview.setHasFixedSize(false)
      applistRecyclerview.addItemDecoration(dividerDecoration)
      applistRecyclerview.adapter = FastAdapter.with<LockListItem, ModelAdapter<AppEntry, LockListItem>>(
          adapter)

      adapter.fastAdapter.apply {
        withSelectable(true)
        withOnClickListener { _, _, item, _ ->
          displayLockInfoFragment(item.model)
          return@withOnClickListener true
        }
      }

      applistEmpty.visibility = View.GONE
      applistRecyclerview.visibility = View.VISIBLE
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.apply {
      inflate(R.menu.locklist_menu, menu)
      inflate(R.menu.search_menu, menu)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    setupDisplaySystemVisibleItem(menu)
    filterListDelegate.onPrepareOptionsMenu(menu, adapter)
  }

  private fun setupDisplaySystemVisibleItem(menu: Menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system)
    presenter.setSystemVisibilityFromPreference()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    fabIconTask = LoaderHelper.unload(fabIconTask)
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
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_is_system -> if (!binding.applistSwipeRefresh.isRefreshing) {
        Timber.d("List is not refreshing. Allow change of system preference")
        presenter.setSystemVisibility(!item.isChecked)
        presenter.populateList(true)
      }
      else -> Timber.w("Item selected: %d, do nothing", item.itemId)
    }
    return super.onOptionsItemSelected(item)
  }

  private fun setupFAB() {
    binding.applistFab.setOnDebouncedClickListener {
      if (PadLockService.isRunning) {
        DialogUtil.guaranteeSingleDialogFragment(activity,
            PinEntryDialog.newInstance(context!!.packageName), PinEntryDialog.TAG)
      } else {
        DialogUtil.guaranteeSingleDialogFragment(activity, AccessibilityRequestDialog(),
            "accessibility")
      }
    }
    FABUtil.setupFABBehavior(binding.applistFab, HideScrollFABBehavior(24))
  }

  private fun displayLockInfoFragment(entry: AppEntry) {
    DialogUtil.guaranteeSingleDialogFragment(activity, LockInfoDialog.newInstance(entry),
        LockInfoDialog.TAG)
  }

  override fun onMasterPinCreateSuccess() {
    onFABEnabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun onMasterPinCreateFailure() {
    Toasty.makeText(context!!, "Error: Mismatched PIN", Toast.LENGTH_SHORT).show()
  }

  override fun onMasterPinClearSuccess() {
    onFABDisabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun onMasterPinClearFailure() {
    Toasty.makeText(context!!, "Error: Invalid PIN", Toast.LENGTH_SHORT).show()
  }

  private fun setRefreshing(refresh: Boolean) {
    binding.applistSwipeRefresh.refreshing(refresh)
    activity?.invalidateOptionsMenu()
    filterListDelegate.setEnabled(!refresh)

    if (refresh) {
      binding.applistFab.hide()
    } else {
      binding.applistFab.show()
    }
  }

  private fun refreshList(packageName: String, locked: Boolean? = null,
      whitelisted: Boolean? = null, hardlocked: Boolean? = null) {
    for (i in adapter.adapterItems.indices) {
      val item: LockListItem = adapter.getAdapterItem(i)
      val entry: AppEntry = item.model
      if (packageName == entry.packageName) {
        val newLocked: Boolean = locked ?: entry.locked
        val newWhitelisted: Int = maxOf(0, when {
          whitelisted == null -> entry.whitelisted
          whitelisted -> entry.whitelisted + 1
          else -> entry.whitelisted - 1
        })
        val newHardLocked: Int = maxOf(0, when {
          hardlocked == null -> entry.hardLocked
          hardlocked -> entry.hardLocked + 1
          else -> entry.hardLocked - 1
        })

        if (item.updateModel(
            AppEntry(name = entry.name, packageName = entry.packageName, system = entry.system,
                locked = newLocked, whitelisted = newWhitelisted, hardLocked = newHardLocked))) {
          adapter.fastAdapter.notifyAdapterItemChanged(i)
        }

        // Update cache with the whitelist numbers so that a soft refresh will not change visual
        presenter.updateCache(packageName, newWhitelisted, newHardLocked)
        break
      }
    }
  }

  override fun onModifyEntryCreated(packageName: String) {
    Timber.d("Created entry for $packageName")
    refreshList(packageName = packageName, locked = true, whitelisted = null, hardlocked = null)
  }

  override fun onModifyEntryDeleted(packageName: String) {
    Timber.d("Deleted entry for $packageName")
    refreshList(packageName = packageName, locked = false, whitelisted = null, hardlocked = null)
  }

  override fun onModifyEntryError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }

  override fun onModifySubEntryToDefaultFromWhitelisted(packageName: String) {
    Timber.d("Defaulted from whitelist subentry for $packageName")
    refreshList(packageName = packageName, locked = null, whitelisted = false, hardlocked = null)
  }

  override fun onModifySubEntryToDefaultFromHardlocked(packageName: String) {
    Timber.d("Defaulted from hardlock subentry for $packageName")
    refreshList(packageName = packageName, locked = null, whitelisted = null, hardlocked = false)
  }

  override fun onModifySubEntryToWhitelistedFromDefault(packageName: String) {
    Timber.d("Whitelisted from default subentry for $packageName")
    refreshList(packageName = packageName, locked = null, whitelisted = true, hardlocked = null)
  }

  override fun onModifySubEntryToWhitelistedFromHardlocked(packageName: String) {
    Timber.d("Whitelisted from hardlock subentry for $packageName")
    refreshList(packageName = packageName, locked = null, whitelisted = true, hardlocked = false)
  }

  override fun onModifySubEntryToHardlockedFromDefault(packageName: String) {
    Timber.d("Hardlocked from default subentry for $packageName")
    refreshList(packageName = packageName, locked = null, whitelisted = null, hardlocked = true)
  }

  override fun onModifySubEntryToHardlockedFromWhitelisted(packageName: String) {
    Timber.d("Hardlocked from whitelisted subentry for $packageName")
    refreshList(packageName = packageName, locked = null, whitelisted = false, hardlocked = true)
  }

  override fun onModifySubEntryError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }

  override fun onFABEnabled() {
    fabIconTask = LoaderHelper.unload(fabIconTask)
    fabIconTask = imageLoader.fromResource(R.drawable.ic_lock_outline_24dp)
        .into(binding.applistFab)
  }

  override fun onFABDisabled() {
    fabIconTask = LoaderHelper.unload(fabIconTask)
    fabIconTask = imageLoader.fromResource(R.drawable.ic_lock_open_24dp)
        .into(binding.applistFab)
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
    setRefreshing(true)
    backingSet.clear()
  }

  override fun onEntryAddedToList(entry: AppEntry) {
    backingSet.add(entry)

    var update = false
    for (index in adapter.adapterItems.indices) {
      val item: LockListItem = adapter.adapterItems[index]
      if (item.model.packageName == entry.packageName) {
        update = true
        if (item.updateModel(entry)) {
          adapter.fastAdapter.notifyAdapterItemChanged(index)
        }
        break
      }
    }

    if (!update) {
      binding.apply {
        applistEmpty.visibility = View.GONE
        applistRecyclerview.visibility = View.VISIBLE
      }

      var added = false
      for (index in adapter.adapterItems.indices) {
        val item: LockListItem = adapter.adapterItems[index]
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

  override fun onListPopulated() {
    adapter.retainAll(backingSet)
    if (adapter.adapterItemCount > 0) {
      binding.applistEmpty.visibility = View.GONE
      binding.applistRecyclerview.visibility = View.VISIBLE
      Timber.d("We have refreshed")
      presenter.showOnBoarding()

      lastPosition = ListStateUtil.restorePosition(lastPosition, binding.applistRecyclerview)
    } else {
      binding.applistRecyclerview.visibility = View.GONE
      binding.applistEmpty.visibility = View.VISIBLE
      Toasty.makeText(context!!, "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show()
    }

    setRefreshing(false)
  }

  override fun onListPopulateError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }

  companion object {

    const val TAG = "LockListFragment"
  }
}
