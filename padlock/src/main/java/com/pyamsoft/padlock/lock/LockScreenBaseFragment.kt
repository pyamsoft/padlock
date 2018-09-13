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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import com.google.android.material.snackbar.Snackbar
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.list.ErrorDialog
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_ACTIVITY_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_IS_SYSTEM
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_LOCK_CODE
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_PACKAGE_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_REAL_NAME
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.LOCKED
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.POSTED
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.SUBMIT_FAILURE
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.SUBMIT_SUCCESS
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarFragment
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import com.pyamsoft.pydroid.ui.app.fragment.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.ui.util.show
import timber.log.Timber
import javax.inject.Inject

abstract class LockScreenBaseFragment protected constructor() : ToolbarFragment() {

  @field:Inject
  internal lateinit var viewModel: LockViewModel

  @field:Inject
  internal lateinit var imageLoader: ImageLoader

  private lateinit var lockedActivityName: String
  private lateinit var lockedPackageName: String
  private lateinit var lockedRealName: String
  private var lockedCode: String? = null
  private var isLockedSystem: Boolean = false

  private fun showSnackbarWithText(text: String) {
    val activity = activity
    if (activity is LockScreenActivity) {
      Snackbreak.make(activity.getRootView(), text, Snackbar.LENGTH_SHORT)
          .show()
    }
  }

  private val isExcluded: Boolean
    @CheckResult get() {
      val activity = activity
      return activity is LockScreenActivity && activity.isExcluded()
    }

  private val selectedIgnoreTime: Long
    @CheckResult get() {
      val activity = activity
      return (activity as? LockScreenActivity)?.getIgnoreTimeFromSelectedIndex() ?: 0
    }

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    requireArguments().let {
      lockedPackageName = it.getString(ENTRY_PACKAGE_NAME, "")
      lockedActivityName = it.getString(ENTRY_ACTIVITY_NAME, "")
      lockedRealName = it.getString(ENTRY_REAL_NAME, "")
      lockedCode = it.getString(ENTRY_LOCK_CODE)
      isLockedSystem = it.getBoolean(ENTRY_IS_SYSTEM, false)
    }

    require(lockedPackageName.isNotBlank())
    require(lockedActivityName.isNotBlank())
    require(lockedRealName.isNotBlank())
  }

  @CallSuper
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val module =
      LockEntryModule(viewLifecycleOwner, lockedPackageName, lockedActivityName, lockedRealName)
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockScreenComponent(module)
        .inject(this)

    viewModel.onHintDisplay { onDisplayHint(it) }
    viewModel.onLockStageBusEvent { wrapper ->
      wrapper.onError {
        clearDisplay()
        ErrorDialog().show(requireActivity(), "lock_screen_text_error")
      }

      wrapper.onSuccess {
        clearDisplay()
        when (it) {
          SUBMIT_FAILURE -> {
            Timber.d("Submit failure")
            showSnackbarWithText("Error: Invalid PIN")
          }
          SUBMIT_SUCCESS -> {
            Timber.d("Submit success")
          }
          LOCKED -> {
            Timber.d("Locked out")
            showSnackbarWithText("This entry is temporarily locked")
          }
          POSTED -> {
            Timber.d("Unlock posted")
            requireActivity().finish()
          }
        }
      }
    }

    // Base provides no view
    return null
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar { it.setUpEnabled(false) }
  }

  internal abstract fun onRestoreInstanceState(savedInstanceState: Bundle)

  protected abstract fun clearDisplay()

  protected abstract fun onDisplayHint(hint: String)

  protected fun submitPin(currentAttempt: String) {
    viewModel.submit(lockedCode, currentAttempt, isLockedSystem, isExcluded, selectedIgnoreTime)
  }

  companion object {

    @JvmStatic
    @CheckResult
    internal fun buildBundle(
      lockedPackageName: String,
      lockedActivityName: String,
      lockedCode: String?,
      lockedRealName: String,
      lockedSystem: Boolean
    ): Bundle {
      val args = Bundle()
      args.putString(ENTRY_PACKAGE_NAME, lockedPackageName)
      args.putString(ENTRY_ACTIVITY_NAME, lockedActivityName)
      args.putString(ENTRY_LOCK_CODE, lockedCode)
      args.putString(ENTRY_REAL_NAME, lockedRealName)
      args.putBoolean(ENTRY_IS_SYSTEM, lockedSystem)
      return args
    }
  }
}
