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
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
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
import com.pyamsoft.pydroid.ui.util.AnimUtil
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockListFragment : CanaryFragment(), LockListPresenter.BusCallback {

  @field:Inject internal lateinit var presenter: LockListPresenter
  private lateinit var fastItemAdapter: FastItemAdapter<LockListItem>
  private lateinit var binding: FragmentLockListBinding
  private lateinit var filterListDelegate: FilterListDelegate
  private var fabIconTask = LoaderHelper.empty()
  private var dividerDecoration: DividerItemDecoration? = null
  private var lastPosition: Int = 0

  private val onFabStateEnabled: () -> Unit = {
    fabIconTask = LoaderHelper.unload(fabIconTask)
    fabIconTask = ImageLoader.fromResource(activity, R.drawable.ic_lock_outline_24dp)
        .into(binding.applistFab)
  }

  private val onFabStateDisabled: () -> Unit = {
    fabIconTask = LoaderHelper.unload(fabIconTask)
    fabIconTask = ImageLoader.fromResource(activity, R.drawable.ic_lock_open_24dp)
        .into(binding.applistFab)
  }

  private val onListPopulateBegin: () -> Unit = {
    setRefreshing(true)
    fastItemAdapter.clear()
    binding.applistFab.hide()

    binding.applistEmpty.visibility = View.GONE
    binding.applistRecyclerview.visibility = View.GONE
  }

  private val onEntryAddedToList: (AppEntry) -> Unit = {
    fastItemAdapter.add(LockListItem(activity, it))
  }

  private val onListPopulated: () -> Unit = {
    setRefreshing(false)
    binding.applistFab.show()

    if (fastItemAdapter.adapterItemCount > 0) {
      binding.applistEmpty.visibility = View.GONE
      binding.applistRecyclerview.visibility = View.VISIBLE
      Timber.d("We have refreshed")
      presenter.showOnBoarding(onOnboardingComplete = {
        Timber.d("onboarding complete")
      }, onShowOnboarding = {
        Timber.d("Show onboarding")
      })

      lastPosition = ListStateUtil.restorePosition(lastPosition, binding.applistRecyclerview)
    } else {
      binding.applistRecyclerview.visibility = View.GONE
      binding.applistEmpty.visibility = View.VISIBLE
      Toasty.makeText(context, "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show()
    }
  }

  private val onListPopulateError: (Throwable) -> Unit = {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }


  override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

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

    lastPosition = ListStateUtil.restoreState(savedInstanceState)

    presenter.bind(this)

    val intent = activity.intent
    if (intent.hasExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST)) {
      intent.removeExtra(ApplicationInstallReceiver.FORCE_REFRESH_LIST)

      Timber.d("Launched from notification, clear list")
      presenter.forceClearCache()
    }
  }

  private fun modifyList(packageName: String, checked: Boolean) {
    for (i in fastItemAdapter.adapterItems.indices) {
      val item: LockListItem = fastItemAdapter.getAdapterItem(i)
      if (packageName == item.model.packageName()) {
        fastItemAdapter.set(i,
            LockListItem(activity, item.model.toBuilder().locked(checked).build()))
        break
      }
    }
  }

  override fun onEntryCreated(packageName: String) {
    Timber.d("Created entry for $packageName")
    modifyList(packageName, true)
  }

  override fun onEntryDeleted(packageName: String) {
    Timber.d("Deleted entry for $packageName")
    modifyList(packageName, false)
  }

  override fun onEntryError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "list_error")
  }

  override fun onStart() {
    super.onStart()
    presenter.populateList(false, onListPopulateBegin, onEntryAddedToList,
        onListPopulated, onListPopulateError)
    presenter.setFABStateFromPreference(onFabStateEnabled, onFabStateDisabled)
  }

  override fun onStop() {
    super.onStop()
    lastPosition = ListStateUtil.getCurrentPosition(binding.applistRecyclerview)
  }

  override fun onSaveInstanceState(outState: Bundle?) {
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
    binding.applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500)
    binding.applistSwipeRefresh.setOnRefreshListener {
      Timber.d("onRefresh")
      presenter.populateList(true, onListPopulateBegin, onEntryAddedToList,
          onListPopulated, onListPopulateError)
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
        presenter.setSystemVisibility(!item.isChecked)
        presenter.populateList(true, onListPopulateBegin, onEntryAddedToList,
            onListPopulated, onListPopulateError)
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
    onFabStateEnabled()
    val v = view
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show()
    }
  }

  override fun onMasterPinCreateFailure() {
    Toasty.makeText(context, "Error: Mismatched PIN", Toast.LENGTH_SHORT).show()
  }

  override fun onMasterPinClearSuccess() {
    onFabStateDisabled()
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

  companion object {

    const val TAG = "LockListFragment"
  }
}
