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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.DISABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.ENABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PAUSED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PERMISSION
import com.pyamsoft.padlock.api.service.ServiceManager
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.padlock.model.list.ListDiffProvider
import com.pyamsoft.padlock.pin.ClearPinPresenter
import com.pyamsoft.padlock.pin.ConfirmPinPresenter
import com.pyamsoft.padlock.pin.CreatePinPresenter
import com.pyamsoft.padlock.pin.PinConfirmDialog
import com.pyamsoft.padlock.pin.PinCreateDialog
import com.pyamsoft.padlock.service.device.UsagePermissionChecker
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.app.toolbarActivity
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

class LockListFragment : Fragment(),
    CreatePinPresenter.Callback,
    ConfirmPinPresenter.Callback,
    ClearPinPresenter.Callback {

  @field:Inject internal lateinit var viewModel: LockListViewModel
  @field:Inject internal lateinit var serviceManager: ServiceManager
  @field:Inject internal lateinit var lockView: LockListView

  @field:Inject internal lateinit var createPinPresenter: CreatePinPresenter
  @field:Inject internal lateinit var confirmPinPresenter: ConfirmPinPresenter
  @field:Inject internal lateinit var clearPinPresenter: ClearPinPresenter

  private var visibilityDisposable by singleDisposable()
  private var databaseChangeDisposable by singleDisposable()
  private var lockEventDisposable by singleDisposable()
  private var fabStateChangeDisposable by singleDisposable()
  private var fabStateCheckDisposable by singleDisposable()
  private var populateDisposable by singleDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockListComponent()
        .activity(requireActivity())
        .toolbarActivity(requireToolbarActivity())
        .listStateTag(TAG)
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .diffProvider(object : ListDiffProvider<AppEntry> {
          override fun data(): List<AppEntry> = lockView.getListData()
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

    lockView.onFabClicked { checkFabState(true) }

    lockView.onToolbarMenuItemClicked {
      if (it.itemId == R.id.menu_is_system) {
        viewModel.setSystemVisibility(!it.isChecked)
        populateList(true)
      }
    }

    lockView.onListItemClicked { displayLockInfoFragment(it) }

    lockView.onRefreshed { checkFabState(false) }

    visibilityDisposable = viewModel.onSystemVisibilityChanged {
      lockView.onSystemVisibilityChanged(it)
    }

    lockEventDisposable = viewModel.onLockEvent(
        onWhitelist = { populateList(true) },
        onError = { lockView.onModifyEntryError { populateList(true) } }
    )

    databaseChangeDisposable = viewModel.onDatabaseChangeEvent(
        onChange = { lockView.onDatabaseChangeReceived(it.index, it.entry) },
        onError = { ErrorDialog().show(requireActivity(), "db_change_error") }
    )

    fabStateChangeDisposable = viewModel.onFabStateChange {
      onFabStateChanged(it, false)
    }

    val intent = requireActivity().intent
    if (intent.hasExtra(ServiceManager.FORCE_REFRESH_ON_OPEN)) {
      intent.removeExtra(ServiceManager.FORCE_REFRESH_ON_OPEN)

      Timber.d("Launched from notification, force list refresh")
      populateList(true)
    }

    createPinPresenter.bind(viewLifecycleOwner, this)
    confirmPinPresenter.bind(viewLifecycleOwner, this)
  }

  private fun onFabStateChanged(
    state: ServiceState,
    fromClick: Boolean
  ) {
    when (state) {
      ENABLED -> onFabIconLocked(fromClick)
      DISABLED -> onFabIconUnlocked(fromClick)
      PERMISSION -> onFabIconPermissionDenied(fromClick)
      PAUSED -> onFabIconPaused(fromClick)
    }
  }

  private fun populateList(forced: Boolean) {
    populateDisposable = viewModel.populateList(forced,
        onPopulateBegin = { lockView.onListPopulateBegin() },
        onPopulateSuccess = { lockView.onListLoaded(it) },
        onPopulateError = { lockView.onListPopulateError { populateList(true) } },
        onPopulateComplete = { lockView.onListPopulated() }
    )
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    lockView.commitListState(outState)
  }

  override fun onStart() {
    super.onStart()
    populateList(false)
    checkFabState(false)
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar {
      it.setTitle(R.string.app_name)
      it.setUpEnabled(false)
    }
  }

  override fun onPause() {
    super.onPause()
    lockView.commitListState(null)

    if (isRemoving) {
      toolbarActivity?.withToolbar {
        it.menu.apply {
          removeGroup(R.id.menu_group_list_system)
          removeGroup(R.id.menu_group_list_search)
        }
        it.setOnMenuItemClickListener(null)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    fabStateCheckDisposable.tryDispose()
    populateDisposable.tryDispose()
    fabStateChangeDisposable.tryDispose()
    databaseChangeDisposable.tryDispose()
    lockEventDisposable.tryDispose()
    visibilityDisposable.tryDispose()
  }

  private fun checkFabState(fromClick: Boolean) {
    fabStateCheckDisposable = viewModel.checkFabState {
      onFabStateChanged(it, fromClick)
    }
  }

  private fun displayLockInfoFragment(entry: AppEntry) {
    LockInfoDialog.newInstance(entry)
        .show(requireActivity(), LockInfoDialog.TAG)
  }

  private fun onFabIconLocked(fromClick: Boolean) {
    lockView.onFabIconLocked()

    if (fromClick) {
      if (UsagePermissionChecker.hasPermission(requireContext())) {
        PinConfirmDialog.newInstance(finishOnDismiss = false)
            .show(requireActivity(), PinConfirmDialog.TAG)
      }
    }
  }

  private fun onFabIconUnlocked(fromClick: Boolean) {
    lockView.onFabIconUnlocked()

    if (fromClick) {
      if (UsagePermissionChecker.hasPermission(requireContext())) {
        PinCreateDialog.newInstance()
            .show(requireActivity(), PinCreateDialog.TAG)
      }
    }
  }

  private fun onFabIconPermissionDenied(fromClick: Boolean) {
    lockView.onFabIconPermissionDenied()

    if (fromClick) {
      UsageAccessRequestDialog().show(requireActivity(), "usage_access")
    }
  }

  private fun onFabIconPaused(fromClick: Boolean) {
    lockView.onFabIconPaused()

    if (fromClick) {
      serviceManager.startService(true)
    }
  }

  override fun onConfirmPinBegin() {
  }

  override fun onConfirmPinSuccess(attempt: String) {
    Timber.d("Pin confirm success")
    clearPinPresenter.clear(attempt)
  }

  override fun onConfirmPinFailure(attempt: String) {
    Timber.d("Pin confirm failed")
    lockView.onMasterPinClearFailure()
  }

  override fun onConfirmPinComplete() {
  }

  override fun onCreatePinBegin() {
  }

  override fun onCreatePinSuccess() {
    Timber.d("Pin create success")
    lockView.onMasterPinCreateSuccess()
  }

  override fun onCreatePinFailure() {
    Timber.d("Pin create failure")
    lockView.onMasterPinCreateFailure()
  }

  override fun onCreatePinComplete() {
  }

  override fun onPinClearSuccess() {
    Timber.d("Pin clear success")
    lockView.onMasterPinClearSuccess()
  }

  override fun onPinClearFailed() {
    Timber.d("Pin clear failure")
    lockView.onMasterPinClearFailure()
  }

  companion object {

    const val TAG = "LockListFragment"
  }
}
