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

package com.pyamsoft.padlock.pin

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.lock.screen.PinScreenInputViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import com.pyamsoft.pydroid.ui.util.commit
import timber.log.Timber
import javax.inject.Inject

class PinDialog : ToolbarDialog() {

  @field:Inject internal lateinit var pinView: PinView
  @field:Inject internal lateinit var viewModel: PinScreenInputViewModel

  private var checkOnly: Boolean = false
  private var finishOnDismiss: Boolean = false

  private var lockScreenTypeDisposable by singleDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true

    checkOnly = requireArguments().getBoolean(CHECK_ONLY, false)
    finishOnDismiss = requireArguments().getBoolean(FINISH_ON_DISMISS, false)
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

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPinComponent()
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .build()
        .inject(this)

    pinView.create()
    return pinView.root()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    pinView.onToolbarNavigationClicked { dismiss() }

    pinView.onToolbarMenuItemClicked {
      when (it) {
        R.id.menu_submit_pin -> {
          val fragmentManager = childFragmentManager
          val fragment: Fragment? =
            fragmentManager.findFragmentById(R.id.pin_entry_dialog_container)
          if (fragment is PinBaseFragment) {
            fragment.onSubmitPressed()
          }
        }
      }
    }

    lockScreenTypeDisposable = viewModel.resolveLockScreenType(
        onTypePattern = { onTypePattern() },
        onTypeText = { onTypeText() }
    )
  }

  private fun pushIfNotPresent(
    pushFragment: PinBaseFragment,
    tag: String
  ) {
    val fragmentManager = childFragmentManager
    val fragment = fragmentManager.findFragmentByTag(tag)
    if (fragment == null) {
      Timber.d("Push new pin fragment: $tag")
      fragmentManager.beginTransaction()
          .add(R.id.pin_entry_dialog_container, pushFragment, tag)
          .commit(viewLifecycleOwner)
    }
  }

  private fun onTypePattern() {
    // Push text as child fragment
    Timber.d("Type Pattern")
    pushIfNotPresent(PinPatternFragment.newInstance(checkOnly), PinPatternFragment.TAG)
  }

  private fun onTypeText() {
    Timber.d("Type Text")
    pushIfNotPresent(PinTextFragment.newInstance(checkOnly), PinTextFragment.TAG)
  }

  override fun onDismiss(dialog: DialogInterface?) {
    super.onDismiss(dialog)
    if (finishOnDismiss) {
      activity?.also { it.finish() }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("Destroy AlertDialog")
    lockScreenTypeDisposable.tryDispose()
  }

  companion object {

    const val TAG = "PinDialog"
    internal const val CHECK_ONLY = "check_only"
    private const val FINISH_ON_DISMISS = "finish_dismiss"

    @JvmStatic
    @CheckResult
    fun newInstance(
      checkOnly: Boolean,
      finishOnDismiss: Boolean
    ): PinDialog {
      return PinDialog().apply {
        arguments = Bundle().apply {
          putBoolean(CHECK_ONLY, checkOnly)
          putBoolean(FINISH_ON_DISMISS, finishOnDismiss)
        }
      }
    }
  }
}
