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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.padlock.lock.screen.PinScreenInputViewModel
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import javax.inject.Inject

class PinDialog : ToolbarDialog() {

  @field:Inject internal lateinit var viewModel: PinScreenInputViewModel
  @field:Inject internal lateinit var imageLoader: ImageLoader
  @field:Inject internal lateinit var appIconLoader: AppIconLoader
  @field:Inject internal lateinit var theming: Theming

  private lateinit var binding: DialogPinEntryBinding

  private var checkOnly: Boolean = false
  private var finishOnDismiss: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true
    checkOnly = requireArguments().getBoolean(CHECK_ONLY, false)
    finishOnDismiss = requireArguments().getBoolean(FINISH_ON_DISMISS, false)
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

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .plusPinComponent(PinModule(viewLifecycleOwner))
        .inject(this)

    binding = DialogPinEntryBinding.inflate(inflater, container, false)

    setupToolbar()

    viewModel.onLockScreenTypePattern { onTypePattern() }
    viewModel.onLockScreenTypeText { onTypeText() }
    viewModel.resolveLockScreenType()

    return binding.root
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

  override fun onStart() {
    super.onStart()
    appIconLoader.loadAppIcon(requireContext().packageName, R.mipmap.ic_launcher)
        .into(binding.pinImage)
        .bind(viewLifecycleOwner)
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

  private fun applyBackNavigationIcon(icon: Drawable) {
    val color: Int
    if (theming.isDarkTheme()) {
      color = R.color.white
    } else {
      color = R.color.black
    }
    val tint = ContextCompat.getColor(binding.pinEntryToolbar.context, color)
    binding.pinEntryToolbar.navigationIcon = icon.tintWith(tint)
  }

  private fun setupToolbar() {
    // Maybe something more descriptive
    binding.apply {
      if (theming.isDarkTheme()) {
        pinEntryToolbar.popupTheme = R.style.ThemeOverlay_AppCompat
      } else {
        pinEntryToolbar.popupTheme = R.style.ThemeOverlay_AppCompat_Light
      }

      pinEntryToolbar.title = "PIN"
      pinEntryToolbar.setNavigationOnClickListener(DebouncedOnClickListener.create { dismiss() })

      // Set up icon as black
      if (finishOnDismiss) {
        // Load a custom X icon if dismissing the dialog closes the activity
        imageLoader.load(R.drawable.ic_close_24dp)
            .into(object : ImageTarget<Drawable> {
              override fun clear() {
                pinEntryToolbar.navigationIcon = null
              }

              override fun setError(error: Drawable?) {
                pinEntryToolbar.navigationIcon = error
              }

              override fun setImage(image: Drawable) {
                applyBackNavigationIcon(image)
              }

              override fun setPlaceholder(placeholder: Drawable?) {
                pinEntryToolbar.navigationIcon = placeholder
              }

              override fun view(): View {
                return pinEntryToolbar
              }

            })
            .bind(viewLifecycleOwner)
      } else {
        // Otherwise the usual back arrow is fine
        pinEntryToolbar.setUpEnabled(true)
        pinEntryToolbar.navigationIcon?.also { applyBackNavigationIcon(it) }
      }

      // Inflate menu
      pinEntryToolbar.inflateMenu(R.menu.pin_menu)

      val pinItem: MenuItem? = pinEntryToolbar.menu.findItem(R.id.menu_submit_pin)
      if (pinItem != null) {
        var pinIcon: Drawable? = pinItem.icon
        if (pinIcon != null) {
          val color: Int
          if (theming.isDarkTheme()) {
            color = R.color.white
          } else {
            color = R.color.black
          }
          val tint = ContextCompat.getColor(pinEntryToolbar.context, color)
          pinIcon = pinIcon.tintWith(tint)
          pinItem.icon = pinIcon
        }
      }

      pinEntryToolbar.setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menu_submit_pin -> {
            val fragmentManager = childFragmentManager
            val fragment: Fragment? =
              fragmentManager.findFragmentById(R.id.pin_entry_dialog_container)
            if (fragment is PinBaseFragment) {
              fragment.onSubmitPressed()
              return@setOnMenuItemClickListener true
            }
          }
        }
        return@setOnMenuItemClickListener false
      }
    }
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
    binding.unbind()
  }

  companion object {

    const val TAG = "PinDialog"
    internal const val CHECK_ONLY = "check_only"
    private const val FINISH_ON_DISMISS = "check_only"

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
