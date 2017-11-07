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
import android.support.annotation.CheckResult
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.loader.AppIconLoader
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding
import com.pyamsoft.padlock.helper.refreshing
import com.pyamsoft.padlock.helper.retainAll
import com.pyamsoft.padlock.list.info.LockInfoEvent
import com.pyamsoft.padlock.list.info.LockInfoModule
import com.pyamsoft.padlock.list.info.LockInfoPresenter
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.padlock.uicommon.ListStateUtil
import com.pyamsoft.padlock.uicommon.RecyclerViewUtil
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.DialogUtil
import com.pyamsoft.pydroid.util.AppUtil
import timber.log.Timber
import javax.inject.Inject

class LockInfoDialog : CanaryDialog(), LockInfoPresenter.View {

  @field:Inject internal lateinit var appIconLoader: AppIconLoader
  @field:Inject internal lateinit var presenter: LockInfoPresenter
  private lateinit var fastItemAdapter: FastItemAdapter<LockInfoItem>
  private lateinit var binding: DialogLockInfoBinding
  private lateinit var appPackageName: String
  private lateinit var appName: String
  private lateinit var filterListDelegate: FilterListDelegate
  private var appIsSystem: Boolean = false
  private var dividerDecoration: DividerItemDecoration? = null
  private var appIcon = LoaderHelper.empty()
  private var lastPosition: Int = 0
  private val backingSet: MutableSet<ActivityEntry> = LinkedHashSet()

  override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      appPackageName = it.getString(ARG_APP_PACKAGE_NAME, null)
      appName = it.getString(ARG_APP_NAME, null)
      appIsSystem = it.getBoolean(ARG_APP_SYSTEM, false)
    }

    Injector.obtain<PadLockComponent>(context!!.applicationContext).plusLockInfoComponent(
        LockInfoModule(appPackageName)).inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    filterListDelegate = FilterListDelegate()
    fastItemAdapter = FastItemAdapter()
    binding = DialogLockInfoBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupToolbar()
    binding.apply {
      lockInfoPackageName.text = appPackageName
      lockInfoSystem.text = if (appIsSystem) "YES" else "NO"
    }
    setupSwipeRefresh()
    setupRecyclerView()
    filterListDelegate.onViewCreated(fastItemAdapter)
    lastPosition = ListStateUtil.restoreState(savedInstanceState)

    presenter.bind(this)
  }

  private fun setupToolbar() {
    binding.apply {
      lockInfoToolbar.title = appName
      lockInfoToolbar.setNavigationOnClickListener { dismiss() }
      lockInfoToolbar.inflateMenu(R.menu.search_menu)
    }
    ViewCompat.setElevation(binding.lockInfoToolbar,
        AppUtil.convertToDP(binding.lockInfoToolbar.context, 4f))
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.menu, fastItemAdapter)
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      lockInfoSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
          R.color.blue700, R.color.amber500)
      lockInfoSwipeRefresh.setOnRefreshListener {
        Timber.d("onRefresh")
        presenter.populateList(true)
      }
    }
  }

  private fun setupRecyclerView() {
    dividerDecoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)

    binding.apply {
      lockInfoRecycler.layoutManager = LinearLayoutManager(context).apply {
        isItemPrefetchEnabled = true
        initialPrefetchItemCount = 3
      }
      lockInfoRecycler.clipToPadding = false
      lockInfoRecycler.setHasFixedSize(false)
      lockInfoRecycler.addItemDecoration(dividerDecoration)
      lockInfoRecycler.adapter = fastItemAdapter
      lockInfoRecycler.itemAnimator = RecyclerViewUtil.withStandardDurations()

      lockInfoEmpty.visibility = View.GONE
      lockInfoRecycler.visibility = View.VISIBLE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    binding.apply {
      lockInfoRecycler.removeItemDecoration(dividerDecoration)
      lockInfoRecycler.setOnClickListener(null)
      lockInfoRecycler.layoutManager = null
      lockInfoRecycler.adapter = null
      unbind()
    }

    fastItemAdapter.clear()
    backingSet.clear()
  }

  override fun onStart() {
    super.onStart()
    appIcon = LoaderHelper.unload(appIcon)
    appIcon = appIconLoader.forPackageName(appPackageName).into(binding.lockInfoIcon)
    presenter.populateList(false)
  }

  override fun onStop() {
    super.onStop()
    appIcon = LoaderHelper.unload(appIcon)
    lastPosition = ListStateUtil.getCurrentPosition(binding.lockInfoRecycler)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    ListStateUtil.saveState(outState, binding.lockInfoRecycler)
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    val window = dialog.window
    window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT)
  }

  private fun modifyList(id: String, state: LockState) {
    for (i in fastItemAdapter.adapterItems.indices) {
      val item: LockInfoItem = fastItemAdapter.getAdapterItem(i)
      val entry: ActivityEntry = item.model
      if (id == entry.id) {
        if (item.updateModel(
            ActivityEntry(name = entry.name, packageName = entry.packageName, lockState = state))) {
          fastItemAdapter.notifyAdapterItemChanged(i)
        }
        presenter.update(entry.name, entry.packageName, state)
        break
      }
    }
  }

  override fun onListPopulateBegin() {
    binding.lockInfoSwipeRefresh.refreshing(true)
    backingSet.clear()
  }

  override fun onListPopulated() {
    fastItemAdapter.retainAll(backingSet)
    if (fastItemAdapter.adapterItemCount > 0) {
      binding.apply {
        lockInfoEmpty.visibility = View.GONE
        lockInfoRecycler.visibility = View.VISIBLE
      }

      Timber.d("Refresh finished")
      presenter.showOnBoarding()

      lastPosition = ListStateUtil.restorePosition(lastPosition, binding.lockInfoRecycler)
    } else {
      binding.apply {
        lockInfoRecycler.visibility = View.GONE
        lockInfoEmpty.visibility = View.VISIBLE
      }
      Toasty.makeText(binding.lockInfoToolbar.context,
          "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show()
    }

    binding.lockInfoSwipeRefresh.refreshing(false)
  }

  override fun onEntryAddedToList(entry: ActivityEntry) {
    backingSet.add(entry)

    var update = false
    for (index in fastItemAdapter.adapterItems.indices) {
      val item: LockInfoItem = fastItemAdapter.adapterItems[index]
      if (item.model.id == entry.id) {
        update = true
        publishLockStateUpdates(item.model, entry)
        if (item.updateModel(entry)) {
          fastItemAdapter.notifyAdapterItemChanged(index)
        }
        break
      }
    }

    if (!update) {
      binding.apply {
        lockInfoEmpty.visibility = View.GONE
        lockInfoRecycler.visibility = View.VISIBLE
      }

      var added = false
      for (index in fastItemAdapter.adapterItems.indices) {
        val item: LockInfoItem = fastItemAdapter.adapterItems[index]
        // The entry should go before this one
        if (entry.name.compareTo(item.model.name, ignoreCase = true) < 0) {
          added = true
          fastItemAdapter.add(index, LockInfoItem(entry, appIsSystem))
          break
        }
      }

      if (!added) {
        // add at the end of the list
        fastItemAdapter.add(LockInfoItem(entry, appIsSystem))
      }
    }
  }

  private fun publishLockStateUpdates(model: ActivityEntry, entry: ActivityEntry) {
    val oldState: LockState = model.lockState
    val newState: LockState = entry.lockState
    if (oldState != newState) {
      Timber.d("Lock state changed for ${entry.packageName} ${entry.name}")
      when (newState) {
        DEFAULT -> presenter.publish(
            LockInfoEvent.Callback.Deleted(entry.id, entry.packageName, oldState))
        LOCKED -> presenter.publish(
            LockInfoEvent.Callback.Created(entry.id, entry.packageName, oldState))
        WHITELISTED -> presenter.publish(
            LockInfoEvent.Callback.Whitelisted(entry.id, entry.packageName, oldState))
        else -> Timber.e("Invalid lock state, do not publish: $newState")
      }
    }
  }

  override fun onListPopulateError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "error")
  }

  override fun onOnboardingComplete() {
    Timber.d("Show onboarding")
  }

  override fun onShowOnboarding() {
    Timber.d("Onboarding complete")
  }

  override fun onModifyEntryCreated(id: String) {
    modifyList(id, LOCKED)
  }

  override fun onModifyEntryDeleted(id: String) {
    modifyList(id, DEFAULT)
  }

  override fun onModifyEntryWhitelisted(id: String) {
    modifyList(id, WHITELISTED)
  }

  override fun onModifyEntryError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "error")
  }

  companion object {

    const internal val TAG = "LockInfoDialog"
    const private val ARG_APP_PACKAGE_NAME = "app_packagename"
    const private val ARG_APP_NAME = "app_name"
    const private val ARG_APP_SYSTEM = "app_system"

    @CheckResult
    @JvmStatic
    fun newInstance(appEntry: AppEntry): LockInfoDialog {
      return LockInfoDialog().apply {
        arguments = Bundle().apply {
          putString(ARG_APP_PACKAGE_NAME, appEntry.packageName)
          putString(ARG_APP_NAME, appEntry.name)
          putBoolean(ARG_APP_SYSTEM, appEntry.system)
        }
      }
    }
  }
}
