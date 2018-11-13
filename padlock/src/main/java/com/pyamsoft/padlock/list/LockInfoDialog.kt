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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CheckResult
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogLockInfoBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.list.info.LockInfoViewModel
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import com.pyamsoft.pydroid.util.tintWith
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

class LockInfoDialog : ToolbarDialog() {

  @field:Inject internal lateinit var appIconLoader: AppIconLoader
  @field:Inject internal lateinit var imageLoader: ImageLoader
  @field:Inject internal lateinit var viewModel: LockInfoViewModel

  private lateinit var adapter: ModelAdapter<ActivityEntry, LockInfoBaseItem<*, *, *>>
  private lateinit var binding: DialogLockInfoBinding
  private lateinit var appPackageName: String
  private lateinit var appName: String
  private lateinit var filterListDelegate: FilterListDelegate
  private lateinit var refreshLatch: RefreshLatch
  private lateinit var listStateTag: String
  private var appIsSystem: Boolean = false
  private var lastPosition: Int = 0
  private var appIcon: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireArguments().also {
      appPackageName = it.getString(ARG_APP_PACKAGE_NAME, "")
      appName = it.getString(ARG_APP_NAME, "")
      appIcon = it.getInt(ARG_APP_ICON, 0)
      appIsSystem = it.getBoolean(ARG_APP_SYSTEM, false)
    }

    require(appPackageName.isNotBlank())
    require(appName.isNotBlank())
    listStateTag = TAG + appPackageName
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockInfoComponent(
            LockInfoProvider(
                viewLifecycleOwner, appPackageName, object : ListDiffProvider<ActivityEntry> {
              override fun data(): List<ActivityEntry> =
                Collections.unmodifiableList(adapter.models)
            })
        )
        .inject(this)

    binding = DialogLockInfoBinding.inflate(inflater, container, false)

    refreshLatch = RefreshLatch.create(viewLifecycleOwner) {
      Timber.d("Posting refresh latch: $it")
      binding.lockInfoSwipeRefresh.refreshing(it)
      filterListDelegate.setEnabled(!it)

      // Load is done
      if (!it) {
        if (adapter.adapterItemCount > 0) {
          showRecycler()
          Timber.d("Refresh finished")

          lastPosition = ListStateUtil.restorePosition(lastPosition, binding.lockInfoRecycler)
        } else {
          binding.apply {
            lockInfoRecycler.visibility = View.GONE
            lockInfoEmpty.visibility = View.VISIBLE
          }
        }
      }
    }
    filterListDelegate = FilterListDelegate()
    adapter = ModelAdapter {
      return@ModelAdapter when (it) {
        is ActivityEntry.Item -> LockInfoItem(it, appIsSystem)
        is ActivityEntry.Group -> LockInfoGroup(appPackageName, it)
      }
    }
    setupToolbar()
    binding.apply {
      lockInfoPackageName.text = appPackageName
      lockInfoSystem.text = if (appIsSystem) "YES" else "NO"
    }
    setupSwipeRefresh()
    setupRecyclerView()
    filterListDelegate.onViewCreated(adapter)
    lastPosition = ListStateUtil.restoreState(listStateTag, savedInstanceState)

    viewModel.onDatabaseChangeEvent { wrapper ->
      wrapper.onSuccess { onDatabaseChangeReceived(it.index, it.entry) }
      wrapper.onError { onDatabaseChangeError() }
    }

    viewModel.onModifyError { onModifyEntryError() }
    viewModel.onPopulateListEvent { wrapper ->
      wrapper.onLoading { onListPopulateBegin() }
      wrapper.onSuccess { onListLoaded(it) }
      wrapper.onError { onListPopulateError() }
      wrapper.onComplete { onListPopulated() }
    }

    return binding.root
  }

  private fun setupToolbar() {
    binding.apply {
      lockInfoToolbar.apply {
        title = appName
        setNavigationOnClickListener(DebouncedOnClickListener.create { dismiss() })
        inflateMenu(R.menu.search_menu)
        inflateMenu(R.menu.lockinfo_menu)

        // Tint search icon white to match Toolbar
        val searchItem = menu.findItem(R.id.menu_search)
        val searchIcon = searchItem.icon
        searchIcon.mutate()
            .also { icon ->
              val tintedIcon = icon.tintWith(requireActivity(), R.color.white)
              searchItem.icon = tintedIcon
            }

        setOnMenuItemClickListener {
          when (it.itemId) {
            R.id.menu_explain_lock_type -> {
              LockInfoExplanationDialog().show(requireActivity(), LockInfoExplanationDialog.TAG)
              return@setOnMenuItemClickListener true
            }
            else -> return@setOnMenuItemClickListener false
          }
        }

        ViewCompat.setElevation(this, 4f.toDp(context).toFloat())
      }
    }
    filterListDelegate.onPrepareOptionsMenu(binding.lockInfoToolbar.menu, adapter)
  }

  private fun setupSwipeRefresh() {
    binding.apply {
      lockInfoSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.blue700)
      lockInfoSwipeRefresh.setOnRefreshListener {
        refreshLatch.forceRefresh()
        viewModel.populateList(true)
      }
    }
  }

  private fun setupRecyclerView() {

    binding.apply {
      lockInfoRecycler.layoutManager = LinearLayoutManager(context)
          .apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = 3
          }
      lockInfoRecycler.setHasFixedSize(true)
      lockInfoRecycler.addItemDecoration(
          DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
      )
      lockInfoRecycler.adapter = FastAdapter.with<
          LockInfoBaseItem<*, *, *>,
          ModelAdapter<ActivityEntry, LockInfoBaseItem<*, *, *>>
          >(adapter)

      // Set initial view state
      lockInfoEmpty.visibility = View.GONE
      lockInfoRecycler.visibility = View.VISIBLE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    filterListDelegate.onDestroyView()
    binding.apply {
      lockInfoRecycler.setOnDebouncedClickListener(null)
      lockInfoRecycler.layoutManager = null
      lockInfoRecycler.adapter = null
      unbind()
    }

    adapter.clear()
  }

  override fun onStart() {
    super.onStart()
    appIconLoader.loadAppIcon(appPackageName, appIcon)
        .into(binding.lockInfoIcon)
        .bind(viewLifecycleOwner)
    viewModel.populateList(false)
  }

  override fun onPause() {
    super.onPause()
    lastPosition = ListStateUtil.getCurrentPosition(binding.lockInfoRecycler)
    ListStateUtil.saveState(listStateTag, null, binding.lockInfoRecycler)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    ListStateUtil.saveState(listStateTag, outState, binding.lockInfoRecycler)
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

  private fun showRecycler() {
    binding.apply {
      lockInfoEmpty.visibility = View.GONE
      lockInfoRecycler.visibility = View.VISIBLE
    }
  }

  private fun onListPopulateBegin() {
    refreshLatch.isRefreshing = true
  }

  private fun onListPopulated() {
    refreshLatch.isRefreshing = false
  }

  private fun onListLoaded(list: List<ActivityEntry>) {
    adapter.set(list)
  }

  private fun onListPopulateError() {
    Snackbreak.long(binding.root, "Failed to load list for $appName")
        .setAction("Retry") { viewModel.populateList(true) }
        .show()
  }

  private fun onModifyEntryError() {
    Snackbreak.long(binding.root, "Failed to modify list for $appName")
        .setAction("Retry") { viewModel.populateList(true) }
        .show()
  }

  private fun onDatabaseChangeReceived(
    index: Int,
    entry: ActivityEntry
  ) {
    adapter.set(index, entry)
  }

  private fun onDatabaseChangeError() {
    ErrorDialog().show(requireActivity(), "db_change_error")
  }

  companion object {

    internal const val TAG = "LockInfoDialog"
    private const val ARG_APP_PACKAGE_NAME = "app_packagename"
    private const val ARG_APP_ICON = "app_icon"
    private const val ARG_APP_NAME = "app_name"
    private const val ARG_APP_SYSTEM = "app_system"

    @CheckResult
    @JvmStatic
    fun newInstance(appEntry: AppEntry): LockInfoDialog {
      return LockInfoDialog().apply {
        arguments = Bundle().apply {
          putString(ARG_APP_PACKAGE_NAME, appEntry.packageName)
          putString(ARG_APP_NAME, appEntry.name)
          putInt(ARG_APP_ICON, appEntry.icon)
          putBoolean(ARG_APP_SYSTEM, appEntry.system)
        }
      }
    }
  }
}
