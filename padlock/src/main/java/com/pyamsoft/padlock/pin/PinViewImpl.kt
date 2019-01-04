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

package com.pyamsoft.padlock.pin

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

internal class PinViewImpl @Inject internal constructor(
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val owner: LifecycleOwner,
  private val imageLoader: ImageLoader,
  private val appIconLoader: AppIconLoader,
  private val theming: Theming
) : PinView, LifecycleObserver {

  private lateinit var binding: DialogPinEntryBinding

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)
    binding.unbind()
  }

  override fun root(): View {
    return binding.root
  }

  override fun create() {
    binding = DialogPinEntryBinding.inflate(inflater, container, false)

    loadAppIcon()
    setupToolbar()
    loadToolbarIcon()
    inflateToolbarMenu()
  }

  private fun loadAppIcon() {
    val packageName = root().context.packageName
    appIconLoader.loadAppIcon(packageName, R.mipmap.ic_launcher)
        .into(binding.pinImage)
        .bind(owner)
  }

  @CheckResult
  private fun tintIcon(icon: Drawable): Drawable {
    val color: Int
    if (theming.isDarkTheme()) {
      color = R.color.white
    } else {
      color = R.color.black
    }
    val tint = ContextCompat.getColor(binding.pinEntryToolbar.context, color)
    return icon.tintWith(tint)
  }

  private fun setupToolbar() {
    // Maybe something more descriptive
    binding.pinEntryToolbar.apply {
      if (theming.isDarkTheme()) {
        popupTheme = R.style.ThemeOverlay_AppCompat
      } else {
        popupTheme = R.style.ThemeOverlay_AppCompat_Light
      }

      title = "PIN"

      // Load a custom X icon
      setUpEnabled(true)
    }
  }

  private fun loadToolbarIcon() {
    imageLoader.load(R.drawable.ic_close_24dp)
        .into(object : ImageTarget<Drawable> {
          override fun clear() {
            binding.pinEntryToolbar.navigationIcon = null
          }

          override fun setError(error: Drawable?) {
            binding.pinEntryToolbar.navigationIcon = error
          }

          override fun setImage(image: Drawable) {
            binding.pinEntryToolbar.navigationIcon = tintIcon(image)
          }

          override fun setPlaceholder(placeholder: Drawable?) {
            binding.pinEntryToolbar.navigationIcon = placeholder
          }

          override fun view(): View {
            return binding.pinEntryToolbar
          }

        })
        .bind(owner)
  }

  private fun inflateToolbarMenu() {
    binding.pinEntryToolbar.apply {
      // Inflate menu
      inflateMenu(R.menu.pin_menu)

      val pinItem: MenuItem? = menu.findItem(R.id.menu_submit_pin)
      if (pinItem != null) {
        val pinIcon: Drawable? = pinItem.icon
        if (pinIcon != null) {
          pinItem.icon = tintIcon(pinIcon)
        }
      }
    }
  }

  override fun onToolbarNavigationClicked(onClick: () -> Unit) {
    binding.pinEntryToolbar.setNavigationOnClickListener(
        DebouncedOnClickListener.create { onClick() })
  }

  override fun onToolbarMenuItemClicked(onClick: (id: Int) -> Unit) {
    binding.pinEntryToolbar.setOnMenuItemClickListener {
      onClick(it.itemId)
      return@setOnMenuItemClickListener true
    }
  }

}