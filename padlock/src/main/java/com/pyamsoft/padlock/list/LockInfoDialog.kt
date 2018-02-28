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
import android.support.annotation.CheckResult
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.Toast
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.AppIconLoader
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.helper.NeverNotifyItemList
import com.pyamsoft.padlock.helper.dispatch
import com.pyamsoft.padlock.list.info.LockInfoPresenter
import com.pyamsoft.padlock.model.ActivityEntry
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.design.util.refreshing
import com.pyamsoft.pydroid.list.ListDiffProvider
import com.pyamsoft.pydroid.list.ListDiffResult
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import com.pyamsoft.pydroid.util.Toasty
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

class LockInfoDialog : CanaryDialog(), LockInfoPresenter.View {

  @field:Inject
  internal lateinit var appIconLoader: AppIconLoader
  @field:Inject
  internal lateinit var presenter: LockInfoPresenter
  private lateinit var adapter: ModelAdapter<ActivityEntry, LockInfoItem>
  private lateinit var binding: DialogLockInfoBinding
  private lateinit var appPackageName: String
  private lateinit var appName: String
  private lateinit var filterListDelegate: FilterListDelegate
  private lateinit var refreshLatch: RefreshLatch
  private var appIsSystem: Boolean = false
  private var dividerDecoration: DividerItemDecoration? = null
  private var lastPosition: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      appPackageName = it.getString(ARG_APP_PACKAGE_NAME, null)
      appName = it.getString(ARG_APP_NAME, null)
      appIsSystem = it.getBoolean(ARG_APP_SYSTEM, false)
    }

    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockInfoComponent(
            LockInfoProvider(appPackageName, object : ListDiffProvider<List<ActivityEntry>> {
              override fun data(): List<ActivityEntry> =
                  Collections.unmodifiableList(adapter.models)
            })
        )
        .inject(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = DialogLockInfoBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    refreshLatch = RefreshLatch.create(viewLifecycle) {
      Timber.d("Posting refresh latch: $it")
      binding.lockInfoSwipeRefresh.refreshing(it)
      filterListDelegate.setEnabled(!it)

      // Load is done
      if (!it) {
        if (adapter.adapterItemCount > 0) {
          showRecycler()
          Timber.d("Refresh finished")
          presenter.showOnBoarding()

          lastPosition = ListStateUtil.restorePosition(lastPosition, binding.lockInfoRecycler)
        } else {
          binding.apply {
            lockInfoRecycler.visibility = View.GONE
            lockInfoEmpty.visibility = View.VISIBLE
          }
          Toasty.makeText(
              binding.lockInfoRecycler.context,
              "Error while loading list. Please try again.",
              Toast.LENGTH_SHORT
          )
        }
      }
    }
    filterListDelegate = FilterListDelegate()
    adapter = ModelAdapter(NeverNotifyItemList.create()) { LockInfoItem(it, appIsSystem) }
    setupToolbar()
    binding.apply {
      lockInfoPackageName.text = appPackageName
      lockInfoSystem.text = if (appIsSystem) "YES" else "NO"
    }
    setupSwipeRefresh()
    setupRecyclerView()
    filterListDelegate.onViewCreated(adapter)
    lastPosition = ListStateUtil.restoreState(TAG, savedInstanceState)

    presenter.bind(viewLifecycle, this)
  }

  private fun setupToolbar() {
    binding.apply {
      lockInfoToolbar.title = appName
      lockInfoToolbar.setNavigationOnClickListener { dismiss() }
      lockInfoToolbar.inflateMenu(R.menu.search_menu)
      lockInfoToolbar.inflateMenu(R.menu.lockinfo_menu)

      lockInfoToolbar.setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_explain_lock_type -> {
            LockInfoExplanationDialog().show(requireActivity(), LockInfoExplanationDialog.TAG)
            return@setOnMenuItemClickListener true
          }
          else -> return@setOnMenuItemClickListener false
        }
      }

      ViewCompat.setElevation(lockInfoToolbar, 4f.toDp(lockInfoToolbar.context).toFloat())
    }
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.menu, adapter)
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      lockInfoSwipeRefresh.setColorSchemeResources(
          R.color.blue500, R.color.amber700,
          R.color.blue700, R.color.amber500
      )
      lockInfoSwipeRefresh.setOnRefreshListener {
        Timber.d("onRefresh")
        refreshLatch.forceRefresh()
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
      lockInfoRecycler.setHasFixedSize(true)
      lockInfoRecycler.addItemDecoration(dividerDecoration)
      lockInfoRecycler.adapter =
          FastAdapter.with<LockInfoItem, ModelAdapter<ActivityEntry, LockInfoItem>>(
              adapter
          )

      // Set initial view state
      lockInfoEmpty.visibility = View.GONE
      lockInfoRecycler.visibility = View.VISIBLE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    binding.apply {
      lockInfoRecycler.removeItemDecoration(dividerDecoration)
      lockInfoRecycler.setOnDebouncedClickListener(null)
      lockInfoRecycler.layoutManager = null
      lockInfoRecycler.adapter = null
      unbind()
    }

    adapter.clear()
  }

  override fun onStart() {
    super.onStart()
    appIconLoader.forPackageName(appPackageName)
        .into(binding.lockInfoIcon)
        .bind(viewLifecycle)
  }

  override fun onPause() {
    super.onPause()
    lastPosition = ListStateUtil.getCurrentPosition(binding.lockInfoRecycler)
    ListStateUtil.saveState(TAG, null, binding.lockInfoRecycler)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    ListStateUtil.saveState(TAG, outState, binding.lockInfoRecycler)
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    val window = dialog.window
    window?.apply {
      setLayout(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT
      )
      setGravity(Gravity.CENTER)
    }
  }

  private fun modifyList(
      id: String,
      state: LockState
  ) {
    for (i in adapter.adapterItems.indices) {
      val item: LockInfoItem = adapter.getAdapterItem(i)
      val entry: ActivityEntry = item.model
      if (id == entry.id) {
        adapter.set(i, ActivityEntry(entry.name, entry.packageName, state))
        presenter.update(entry.name, entry.packageName, state)
        break
      }
    }
  }

  private fun showRecycler() {
    binding.apply {
      lockInfoEmpty.visibility = View.GONE
      lockInfoRecycler.visibility = View.VISIBLE
    }
  }

  override fun onListPopulateBegin() {
    refreshLatch.isRefreshing = true
  }

  override fun onListPopulated() {
    refreshLatch.isRefreshing = false
  }

  override fun onListLoaded(result: ListDiffResult<ActivityEntry>) {
    result.ifEmpty { adapter.clear() }
    result.withValues {
      adapter.setNewList(it.list())
      it.dispatch(adapter)
    }
  }

  override fun onListPopulateError(throwable: Throwable) {
    ErrorDialog().show(activity, "error")
  }

  override fun onOnboardingComplete() {
    Timber.d("Show onboarding")
  }

  override fun onShowOnboarding() {
    Timber.d("Onboarding complete")
  }

  override fun onModifyEntryCreated(id: String) {
    modifyList(id, LockState.LOCKED)
  }

  override fun onModifyEntryDeleted(id: String) {
    modifyList(id, LockState.DEFAULT)
  }

  override fun onModifyEntryWhitelisted(id: String) {
    modifyList(id, LockState.WHITELISTED)
  }

  override fun onModifyEntryError(throwable: Throwable) {
    ErrorDialog().show(activity, "error")
  }

  companion object {

    internal const val TAG = "LockInfoDialog"
    private const val ARG_APP_PACKAGE_NAME = "app_packagename"
    private const val ARG_APP_NAME = "app_name"
    private const val ARG_APP_SYSTEM = "app_system"

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
