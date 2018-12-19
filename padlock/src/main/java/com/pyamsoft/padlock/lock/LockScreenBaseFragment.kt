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
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_ACTIVITY_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_IS_SYSTEM
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_LOCK_CODE
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_PACKAGE_NAME
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_REAL_NAME
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
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

  private lateinit var lockedActivityName: String
  private lateinit var lockedPackageName: String
  private lateinit var lockedRealName: String
  private var lockedCode: String? = null
  private var isLockedSystem: Boolean = false

  private var submitDisposable by singleDisposable()
  private var hintDisposable by singleDisposable()

  private fun showSnackbarWithText(text: String) {
    val activity = activity
    if (activity is LockScreenActivity) {
      Snackbreak.short(activity.getRootView(), text)
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
    val injector = Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusLockScreenComponent(
            LockEntryModule(
                viewLifecycleOwner, lockedPackageName, lockedActivityName, lockedRealName
            )
        )
    injectInto(injector)

    // Base provides no view
    return null
  }

  override fun onResume() {
    super.onResume()
    requireToolbarActivity().withToolbar { it.setUpEnabled(false) }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    submitDisposable.tryDispose()
    hintDisposable.tryDispose()
  }

  protected fun submitPin(currentAttempt: String) {
    submitDisposable =
        viewModel.submit(lockedCode, currentAttempt, isLockedSystem, isExcluded, selectedIgnoreTime,
            onSubmitSuccess = {
              Timber.d("PIN submit success")
              clearDisplay()
            },
            onSubmitFailure = {
              Timber.w("PIN submit failure")
              clearDisplay()

              hintDisposable = viewModel.displayLockedHint { onDisplayHint(it) }
              showSnackbarWithText("Error: Invalid PIN")
            },
            onSubmitResultPostUnlock = {
              Timber.d("Unlock posted")
              clearDisplay()

              requireActivity().finish()
            },
            onSubmitResultAttemptLock = {
              Timber.w("Locked out after bad attempts")
              clearDisplay()

              showSnackbarWithText("This entry is temporarily locked")
            }
        )
  }

  internal abstract fun onRestoreInstanceState(savedInstanceState: Bundle)

  protected abstract fun clearDisplay()

  protected abstract fun onDisplayHint(hint: String)

  protected abstract fun injectInto(injector: LockScreenComponent)

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
