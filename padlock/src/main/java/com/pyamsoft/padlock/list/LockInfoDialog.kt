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
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding
import com.pyamsoft.padlock.list.info.LockInfoModule
import com.pyamsoft.padlock.list.info.LockInfoPresenter
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.model.LockState.WHITELISTED
import com.pyamsoft.padlock.uicommon.AppIconLoader
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.padlock.uicommon.ListStateUtil
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.util.DialogUtil
import com.pyamsoft.pydroid.util.AppUtil
import timber.log.Timber
import javax.inject.Inject

class LockInfoDialog : CanaryDialog(), LockInfoPresenter.View {

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

  override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    appPackageName = arguments.getString(ARG_APP_PACKAGE_NAME, null)
    appName = arguments.getString(ARG_APP_NAME, null)
    appIsSystem = arguments.getBoolean(ARG_APP_SYSTEM, false)

    Injector.obtain<PadLockComponent>(context.applicationContext).plusLockInfoComponent(
        LockInfoModule(appPackageName)).inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    filterListDelegate = FilterListDelegate()
    fastItemAdapter = FastItemAdapter()
    binding = DialogLockInfoBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupToolbar()
    binding.lockInfoPackageName.text = appPackageName
    binding.lockInfoSystem.text = if (appIsSystem) "YES" else "NO"
    setupSwipeRefresh()
    setupRecyclerView()
    filterListDelegate.onViewCreated(fastItemAdapter)
    lastPosition = ListStateUtil.restoreState(savedInstanceState)

    presenter.bind(this)
  }

  private fun setupToolbar() {
    ViewCompat.setElevation(binding.lockInfoToolbar, AppUtil.convertToDP(context, 4f))
    binding.lockInfoToolbar.title = appName
    binding.lockInfoToolbar.setNavigationOnClickListener { dismiss() }
    binding.lockInfoToolbar.inflateMenu(R.menu.search_menu)
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.menu, fastItemAdapter)
  }

  private fun setupSwipeRefresh() {
    binding.lockInfoSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500)
    binding.lockInfoSwipeRefresh.setOnRefreshListener {
      Timber.d("onRefresh")
      presenter.populateList(true)
    }
  }

  private fun setupRecyclerView() {
    dividerDecoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)

    val manager = LinearLayoutManager(context)
    manager.isItemPrefetchEnabled = true
    manager.initialPrefetchItemCount = 3
    binding.lockInfoRecycler.layoutManager = manager
    binding.lockInfoRecycler.clipToPadding = false
    binding.lockInfoRecycler.setHasFixedSize(false)
    binding.lockInfoRecycler.addItemDecoration(dividerDecoration)
    binding.lockInfoRecycler.adapter = fastItemAdapter
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    binding.lockInfoRecycler.removeItemDecoration(dividerDecoration)
    binding.lockInfoRecycler.setOnClickListener(null)
    binding.lockInfoRecycler.layoutManager = null
    binding.lockInfoRecycler.adapter = null
    binding.unbind()
  }

  override fun onStart() {
    super.onStart()
    appIcon = LoaderHelper.unload(appIcon)
    appIcon = AppIconLoader.forPackageName(context, appPackageName).into(binding.lockInfoIcon)
    presenter.populateList(false)
  }

  override fun onStop() {
    super.onStop()
    appIcon = LoaderHelper.unload(appIcon)
    lastPosition = ListStateUtil.getCurrentPosition(binding.lockInfoRecycler)
  }

  override fun onSaveInstanceState(outState: Bundle?) {
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

  private fun setRefreshing(refresh: Boolean) {
    // In case the configuration changes, we do the animation again
    binding.lockInfoSwipeRefresh.post {
      if (binding.lockInfoSwipeRefresh != null) {
        binding.lockInfoSwipeRefresh.isRefreshing = refresh
      }
    }
  }

  private fun modifyList(id: String, state: LockState) {
    for (i in fastItemAdapter.adapterItems.indices) {
      val item: LockInfoItem = fastItemAdapter.getAdapterItem(i)
      if (id == item.model.id()) {
        fastItemAdapter.set(i,
            LockInfoItem(item.model.toBuilder().lockState(state).build(), appIsSystem))
        break
      }
    }
  }

  override fun onListPopulateBegin() {
    setRefreshing(true)
    fastItemAdapter.clear()

    binding.lockInfoEmpty.visibility = View.GONE
    binding.lockInfoRecycler.visibility = View.GONE
  }

  override fun onListPopulated() {
    setRefreshing(false)

    if (fastItemAdapter.adapterItemCount > 0) {
      binding.lockInfoEmpty.visibility = View.GONE
      binding.lockInfoRecycler.visibility = View.VISIBLE

      Timber.d("Refresh finished")
      presenter.showOnBoarding()

      lastPosition = ListStateUtil.restorePosition(lastPosition, binding.lockInfoRecycler)
    } else {
      binding.lockInfoRecycler.visibility = View.GONE
      binding.lockInfoEmpty.visibility = View.VISIBLE
      Toasty.makeText(context, "Error while loading list. Please try again.",
          Toast.LENGTH_SHORT).show()
    }
  }

  override fun onEntryAddedToList(entry: ActivityEntry) {
    fastItemAdapter.add(LockInfoItem(entry, appIsSystem))
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

    fun newInstance(appEntry: AppEntry): LockInfoDialog {
      val fragment = LockInfoDialog()
      val args = Bundle()

      args.putString(ARG_APP_PACKAGE_NAME, appEntry.packageName())
      args.putString(ARG_APP_NAME, appEntry.name())
      args.putBoolean(ARG_APP_SYSTEM, appEntry.system())

      fragment.arguments = args
      return fragment
    }
  }
}
