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

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.DialogInfoLocktypeExplainBinding
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

internal class LockInfoExplanationViewImpl @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val imageLoader: ImageLoader,
  private val theming: Theming
) : LockInfoExplanationView, LifecycleObserver {

  private lateinit var binding: DialogInfoLocktypeExplainBinding

  private var toolbarIconLoaded: Loaded? = null

  init {
    owner.lifecycle.addObserver(this)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    toolbarIconLoaded?.dispose()
    binding.unbind()
  }

  override fun create() {
    binding = DialogInfoLocktypeExplainBinding.inflate(inflater, container, false)

    binding.lockInfoExplainToolbar.setUpEnabled(true)
    loadToolbarIcon()
  }

  override fun onToolbarNavigationClicked(onClick: () -> Unit) {
    binding.lockInfoExplainToolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
      onClick()
    })
  }

  @CheckResult
  private fun tintIcon(icon: Drawable): Drawable {
    val color: Int
    if (theming.isDarkTheme()) {
      color = R.color.white
    } else {
      color = R.color.black
    }
    val tint = ContextCompat.getColor(binding.lockInfoExplainToolbar.context, color)
    return icon.tintWith(tint)
  }

  private fun loadToolbarIcon() {
    toolbarIconLoaded?.dispose()
    toolbarIconLoaded = imageLoader.load(R.drawable.ic_close_24dp)
        .into(object : ImageTarget<Drawable> {
          override fun clear() {
            binding.lockInfoExplainToolbar.navigationIcon = null
          }

          override fun setError(error: Drawable?) {
            binding.lockInfoExplainToolbar.navigationIcon = error
          }

          override fun setImage(image: Drawable) {
            binding.lockInfoExplainToolbar.navigationIcon = tintIcon(image)
          }

          override fun setPlaceholder(placeholder: Drawable?) {
            binding.lockInfoExplainToolbar.navigationIcon = placeholder
          }

          override fun view(): View {
            return binding.lockInfoExplainToolbar
          }

        })
  }

  override fun root(): View {
    return binding.root
  }

}

