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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_IS_SYSTEM
import com.pyamsoft.padlock.lock.LockScreenActivity.Companion.ENTRY_LOCK_CODE
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.requireArguments
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import timber.log.Timber
import javax.inject.Inject

abstract class LockScreenBaseFragment protected constructor() : Fragment() {

  @field:Inject internal lateinit var viewModel: LockViewModel
  @field:Inject internal lateinit var toolbarView: LockToolbarView

  private var isComponentInjected = false

  private var lockedCode: String? = null
  private var isLockedSystem: Boolean = false

  private var submitDisposable by singleDisposable()
  private var hintDisposable by singleDisposable()

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    requireArguments().let {
      lockedCode = it.getString(ENTRY_LOCK_CODE)
      isLockedSystem = it.getBoolean(ENTRY_IS_SYSTEM, false)
    }
  }

  @CallSuper
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    if (!isComponentInjected) {
      throw RuntimeException("Must inject component before onViewCreated()")
    }
  }

  @CheckResult
  protected fun inject(): LockScreenFragmentComponent.Builder {
    isComponentInjected = true
    return Injector.obtain<LockScreenComponent>(requireActivity())
        .plusFragmentComponent()
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
        viewModel.submit(lockedCode, currentAttempt, isLockedSystem,
            toolbarView.isExcludeChecked(),
            toolbarView.getSelectedIgnoreTime(),
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

  protected abstract fun clearDisplay()

  protected abstract fun onDisplayHint(hint: String)

  protected abstract fun showSnackbarWithText(text: String)

  companion object {

    @JvmStatic
    @CheckResult
    internal fun buildBundle(
      lockedCode: String?,
      lockedSystem: Boolean
    ): Bundle {
      val args = Bundle()
      args.putString(ENTRY_LOCK_CODE, lockedCode)
      args.putBoolean(ENTRY_IS_SYSTEM, lockedSystem)
      return args
    }
  }
}
