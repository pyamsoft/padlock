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
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.pin.PinEntryDialog
import com.pyamsoft.padlock.service.PadLockService
import com.pyamsoft.padlock.uicommon.CanaryFragment
import com.pyamsoft.pydroid.design.fab.HideScrollFABBehavior
import com.pyamsoft.pydroid.design.util.FABUtil
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.AnimUtil
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockListFragment : CanaryFragment(), LockListPresenter.Callback {
  @field:Inject internal lateinit var presenter: LockListPresenter
  private lateinit var fastItemAdapter: FastItemAdapter<LockListItem>
  private lateinit var binding: FragmentLockListBinding
  private lateinit var filterListDelegate: FilterListDelegate
  private var fabIconTask = LoaderHelper.empty()
  private var dividerDecoration: DividerItemDecoration? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    Injector.with(context) {
      it.inject(this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    filterListDelegate = FilterListDelegate()
    fastItemAdapter = FastItemAdapter()
    binding = FragmentLockListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    filterListDelegate.onViewCreated(fastItemAdapter)
    setupRecyclerView()
    setupSwipeRefresh()
    setupFAB()
  }

  override fun onStart() {
    super.onStart()
    presenter.start(this)
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
  }

  override fun onResume() {
    super.onResume()
    setActionBarUpEnabled(false)
    AnimUtil.popShow(binding.applistFab, 300, 400)
  }

  override fun onPause() {
    super.onPause()
    AnimUtil.popHide(binding.applistFab, 300, 400)
  }

  private fun setupSwipeRefresh() {
    binding.applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500)
    binding.applistSwipeRefresh.setOnRefreshListener {
      Timber.d("onRefresh")
      presenter.populateList(true, this::onListPopulateBegin, this::onEntryAddedToList,
          this::onListPopulated, this::onListPopulateError)
    }
  }

  private fun setupRecyclerView() {
    dividerDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    fastItemAdapter.withSelectable(true)
    fastItemAdapter.withOnClickListener { _, _, item, _ ->
      displayLockInfoFragment(item.model)
      return@withOnClickListener true
    }

    val manager = LinearLayoutManager(context)
    manager.isItemPrefetchEnabled = true
    manager.initialPrefetchItemCount = 3
    binding.applistRecyclerview.layoutManager = manager
    binding.applistRecyclerview.clipToPadding = false
    binding.applistRecyclerview.setHasFixedSize(false)
    binding.applistRecyclerview.addItemDecoration(dividerDecoration)
    binding.applistRecyclerview.adapter = fastItemAdapter
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.locklist_menu, menu)
    inflater.inflate(R.menu.search_menu, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    setupDisplaySystemVisibleItem(menu)
    filterListDelegate.onPrepareOptionsMenu(menu, fastItemAdapter)
  }

  private fun setupDisplaySystemVisibleItem(menu: Menu) {
    val displaySystemItem: MenuItem = menu.findItem(R.id.menu_is_system)
    presenter.setSystemVisibilityFromPreference { displaySystemItem.isChecked = it }
  }

  override fun onDestroyView() {
    filterListDelegate.onDestroyView()
    binding.applistRecyclerview.removeItemDecoration(dividerDecoration)
    binding.applistRecyclerview.setOnClickListener(null)
    binding.applistRecyclerview.layoutManager = null
    binding.applistRecyclerview.adapter = null
    fastItemAdapter.clear()

    binding.applistFab.setOnClickListener(null)
    binding.applistSwipeRefresh.setOnRefreshListener(null)

    fabIconTask = LoaderHelper.unload(fabIconTask)
    binding.unbind()
    super.onDestroyView()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_is_system -> if (binding.applistSwipeRefresh != null && !binding.applistSwipeRefresh.isRefreshing) {
        Timber.d("List is not refreshing. Allow change of system preference")
        presenter.setSystemVisibility(item.isChecked)
        presenter.populateList(true, this::onListPopulateBegin, this::onEntryAddedToList,
            this::onListPopulated, this::onListPopulateError)
      }
      else -> Timber.w("Item selected: %d, do nothing", item.itemId)
    }
    return super.onOptionsItemSelected(item)
  }

  private fun setupFAB() {
    binding.applistFab.setOnClickListener {
      if (PadLockService.isRunning) {
        DialogUtil.guaranteeSingleDialogFragment(activity,
            PinEntryDialog.newInstance(context.packageName), PinEntryDialog.TAG)
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
    onSetFABStateEnabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun onMasterPinCreateFailure() {
    Toasty.makeText(context, "Error: Mismatched PIN", Toast.LENGTH_SHORT).show()
  }

  override fun onMasterPinClearSuccess() {
    onSetFABStateDisabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun onMasterPinClearFailure() {
    Toasty.makeText(context, "Error: Invalid PIN", Toast.LENGTH_SHORT).show()
  }

  private fun setRefreshing(refresh: Boolean) {
    binding.applistSwipeRefresh.post {
      if (binding.applistSwipeRefresh != null) {
        binding.applistSwipeRefresh.isRefreshing = refresh
      }
    }

    val activity = activity
    if (activity != null) {
      Timber.d("Reload options")
      activity.invalidateOptionsMenu()
    }
  }

  override fun onListPopulateBegin() {
    setRefreshing(true)
    fastItemAdapter.clear()
    binding.applistFab.hide()
  }

  override fun onEntryAddedToList(entry: AppEntry) {
    fastItemAdapter.add(LockListItem(activity, entry))
  }

  override fun onListPopulated() {
    setRefreshing(false)
    binding.applistFab.show()

    if (fastItemAdapter.adapterItemCount > 1) {
      Timber.d("We have refreshed")
      presenter.showOnBoarding(onOnboardingComplete = {
        Timber.d("onboarding complete")
      }, onShowOnboarding = {
        Timber.d("Show onboarding")
      })
    } else {
      Toasty.makeText(context, "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show()
    }
  }

  override fun onListPopulateError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }

  override fun onSetFABStateEnabled() {
    fabIconTask = LoaderHelper.unload(fabIconTask)
    fabIconTask = ImageLoader.fromResource(activity, R.drawable.ic_lock_outline_24dp)
        .into(binding.applistFab)
  }

  override fun onSetFABStateDisabled() {
    fabIconTask = LoaderHelper.unload(fabIconTask)
    fabIconTask = ImageLoader.fromResource(activity, R.drawable.ic_lock_open_24dp)
        .into(binding.applistFab)
  }

  companion object {

    const val TAG = "LockListFragment"
  }
}
