/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.lock.screen.LockScreenPresenter
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.util.DrawableUtil
import timber.log.Timber
import javax.inject.Inject

class PinEntryDialog : CanaryDialog() {

  @field:Inject internal lateinit var presenter: LockScreenPresenter
  private lateinit var binding: DialogPinEntryBinding
  private lateinit var packageName: String
  private var appIcon = LoaderHelper.empty()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    packageName = arguments.getString(ENTRY_PACKAGE_NAME)
    isCancelable = true

    Injector.with(context) {
      it.inject(this)
    }
  }

  override fun onResume() {
    super.onResume()
    // The dialog is super small for some reason. We have to set the size manually, in onResume
    val window = dialog.window
    window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT)
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = DialogPinEntryBinding.inflate(inflater, container, false)
    return binding.root
  }

  private fun pushIfNotPresent(pushFragment: PinEntryBaseFragment,
      tag: String) {
    val fragmentManager = childFragmentManager
    val fragment = fragmentManager.findFragmentByTag(tag)
    if (fragment == null) {
      childFragmentManager.beginTransaction()
          .replace(R.id.pin_entry_dialog_container, pushFragment, tag)
          .commit()
    }
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupToolbar()

    // Start hidden
    binding.pinNextButtonLayout.visibility = View.GONE
    presenter.initializeLockScreenType(onTypePattern = {
      // Push text as child fragment
      binding.pinNextButtonLayout.visibility = View.VISIBLE
      pushIfNotPresent(PinEntryPatternFragment(), PinEntryPatternFragment.TAG)

      binding.pinNextButton.setOnClickListener {
        val fragmentManager = childFragmentManager
        val fragment = fragmentManager.findFragmentByTag(PinEntryPatternFragment.TAG)
        if (fragment is PinEntryPatternFragment) {
          if (fragment.onNextButtonClicked()) {
            binding.pinNextButton.text = "Submit"
          }
        }
      }
    }, onTypeText = {
      // Push text as child fragment
      binding.pinNextButtonLayout.visibility = View.GONE
      pushIfNotPresent(PinEntryTextFragment(), PinEntryTextFragment.TAG)
    })
  }

  override fun onStart() {
    super.onStart()
    appIcon = LoaderHelper.unload(appIcon)
    appIcon = ImageLoader.fromLoader(AppIconLoader.forPackageName(packageName)).into(
        binding.pinImage)
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
    appIcon = LoaderHelper.unload(appIcon)
    binding.pinNextButton.setOnClickListener(null)
  }

  private fun setupToolbar() {
    // Maybe something more descriptive
    binding.pinEntryToolbar.title = "PIN"
    binding.pinEntryToolbar.setNavigationOnClickListener { dismiss() }
    var icon = binding.pinEntryToolbar.navigationIcon
    if (icon != null) {
      icon = DrawableUtil.tintDrawableFromColor(icon,
          ContextCompat.getColor(context, android.R.color.black))
      binding.pinEntryToolbar.navigationIcon = icon
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("Destroy AlertDialog")
    binding.unbind()
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.destroy()
  }

  companion object {

    const val TAG = "PinEntryDialog"
    const private val ENTRY_PACKAGE_NAME = "entry_packagename"

    @JvmStatic @CheckResult fun newInstance(packageName: String): PinEntryDialog {
      val fragment = PinEntryDialog()
      val args = Bundle()
      args.putString(ENTRY_PACKAGE_NAME, packageName)
      fragment.arguments = args
      return fragment
    }
  }
}
