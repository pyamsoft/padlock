/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.list

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.list.info.LockInfoViewModel
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

class LockInfoDialog : ToolbarDialog() {

  @field:Inject internal lateinit var appIconLoader: AppIconLoader
  @field:Inject internal lateinit var viewModel: LockInfoViewModel
  @field:Inject internal lateinit var lockView: LockInfoView

  private var databaseChangeDisposable by singleDisposable()
  private var lockEventDisposable by singleDisposable()
  private var populateDisposable by singleDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val appPackageName = requireArguments().getString(ARG_APP_PACKAGE_NAME, "")
    val appName = requireArguments().getString(ARG_APP_NAME, "")
    val appIcon = requireArguments().getInt(ARG_APP_ICON, 0)
    val appIsSystem = requireArguments().getBoolean(ARG_APP_SYSTEM, false)
    require(appPackageName.isNotBlank())
    require(appName.isNotBlank())
    val listStateTag = TAG + appPackageName

    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockInfoComponent()
        .activity(requireActivity())
        .owner(viewLifecycleOwner)
        .appName(appName)
        .packageName(appPackageName)
        .appIcon(appIcon)
        .appSystem(appIsSystem)
        .listStateTag(listStateTag)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .diffProvider(object : ListDiffProvider<ActivityEntry> {
          override fun data(): List<ActivityEntry> = lockView.getListData()
        })
        .build()
        .inject(this)

    lockView.create()
    return lockView.root()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    lockView.onSwipeRefresh { populateList(true) }

    lockView.onToolbarNavigationClicked { dismiss() }

    lockView.onToolbarMenuItemClicked {
      if (it == R.id.menu_explain_lock_type) {
        LockInfoExplanationDialog().show(requireActivity(), "lock_info_explain")
      }
    }

    databaseChangeDisposable = viewModel.onDatabaseChangeEvent(
        onChange = { lockView.onDatabaseChangeReceived(it.index, it.entry) },
        onError = { lockView.onDatabaseChangeError { populateList(true) } }
    )

    lockEventDisposable = viewModel.onLockEvent(
        onWhitelist = { populateList(true) },
        onError = { lockView.onModifyEntryError { populateList(true) } }
    )
  }

  private fun populateList(forced: Boolean) {
    populateDisposable = viewModel.populateList(forced,
        onPopulateBegin = { lockView.onListPopulateBegin() },
        onPopulateSuccess = { lockView.onListLoaded(it) },
        onPopulateError = { lockView.onListPopulateError { populateList(true) } },
        onPopulateComplete = { lockView.onListPopulated() }
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()

    databaseChangeDisposable.tryDispose()
    lockEventDisposable.tryDispose()
    populateDisposable.tryDispose()
  }

  override fun onStart() {
    super.onStart()
    populateList(false)
  }

  override fun onPause() {
    super.onPause()
    lockView.commitListState(null)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    lockView.commitListState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    dialog.window?.apply {
      setLayout(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT
      )
      setGravity(Gravity.CENTER)
    }
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
