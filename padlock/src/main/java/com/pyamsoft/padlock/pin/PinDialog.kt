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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding
import com.pyamsoft.padlock.loader.loadPadLockIcon
import com.pyamsoft.padlock.lock.screen.LockScreenInputPresenter
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import javax.inject.Inject

class PinDialog : ToolbarDialog(), LockScreenInputPresenter.View {

  @field:Inject
  internal lateinit var presenter: LockScreenInputPresenter
  @field:Inject
  internal lateinit var imageLoader: ImageLoader
  private lateinit var binding: DialogPinEntryBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = true

    Injector.obtain<PadLockComponent>(requireContext().applicationContext)
        .inject(this)
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
    binding = DialogPinEntryBinding.inflate(inflater, container, false)
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
          .add(
              R.id.pin_entry_dialog_container, pushFragment,
              tag
          )
          .commit()
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupToolbar()

    presenter.bind(viewLifecycleOwner, this)
  }

  override fun onStart() {
    super.onStart()
    imageLoader.loadPadLockIcon(requireContext())
        .into(binding.pinImage)
        .bind(viewLifecycleOwner)
  }

  override fun onTypePattern() {
    // Push text as child fragment
    Timber.d("Type Pattern")
    pushIfNotPresent(PinPatternFragment(), PinPatternFragment.TAG)
  }

  override fun onTypeText() {
    Timber.d("Type Text")
    pushIfNotPresent(PinTextFragment(), PinTextFragment.TAG)
  }

  private fun setupToolbar() {
    // Maybe something more descriptive
    binding.apply {
      pinEntryToolbar.title = "PIN"
      pinEntryToolbar.setNavigationOnClickListener { dismiss() }

      // Set up icon as black
      var icon: Drawable? = pinEntryToolbar.navigationIcon
      if (icon != null) {
        icon = icon.tintWith(ContextCompat.getColor(pinEntryToolbar.context, R.color.black))
        pinEntryToolbar.navigationIcon = icon
      }

      // Inflate menu
      pinEntryToolbar.inflateMenu(R.menu.pin_menu)

      // Make icon black
      val pinItem: MenuItem? = pinEntryToolbar.menu.findItem(R.id.menu_submit_pin)
      if (pinItem != null) {
        var pinIcon: Drawable? = pinItem.icon
        if (pinIcon != null) {
          pinIcon = pinIcon.tintWith(
              ContextCompat.getColor(pinEntryToolbar.context, R.color.black)
          )
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

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("Destroy AlertDialog")
    binding.unbind()
  }

  companion object {

    const val TAG = "PinDialog"

  }
}
