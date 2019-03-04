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
import android.view.ViewGroup
import android.widget.ImageView
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject
import javax.inject.Named

internal class LockImageView @Inject internal constructor(
  private val appIconImageLoader: AppIconLoader,
  @Named("locked_package_name") private val packageName: String,
  @Named("locked_app_icon") private val icon: Int,
  parent: ViewGroup
) : BaseUiView<Unit>(parent, Unit) {

  override val layout: Int = R.layout.layout_lock_image

  private val layoutRoot by lazyView<ViewGroup>(R.id.lock_image_root)
  private val lockedIcon by lazyView<ImageView>(R.id.lock_image_icon)

  private var imageBinder: Loaded? = null

  override fun id(): Int {
    return layoutRoot.id
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
    loadAppIcon()
  }

  private fun clearBinder() {
    imageBinder?.dispose()
    imageBinder = null
  }

  private fun loadAppIcon() {
    clearBinder()
    imageBinder = appIconImageLoader.loadAppIcon(packageName, icon)
        .into(lockedIcon)
  }

  override fun teardown() {
    super.teardown()
    clearBinder()
  }
}