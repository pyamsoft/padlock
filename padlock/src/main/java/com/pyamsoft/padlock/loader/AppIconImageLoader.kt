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

package com.pyamsoft.padlock.loader

import android.content.ContentResolver
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.pyamsoft.pydroid.loader.GlideLoader

internal class AppIconImageLoader internal constructor(
  private val packageName: String,
  @DrawableRes private val icon: Int
) : GlideLoader<Drawable>() {

  init {
    if (packageName.isEmpty()) {
      throw IllegalArgumentException("AppIconImageLoader packageName must be non-empty")
    }

    if (icon == 0) {
      throw IllegalArgumentException("AppIconImageLoader icon must be non-zero")
    }
  }

  override fun createRequest(request: RequestManager): RequestBuilder<Drawable> {
    return request.load("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$icon".toUri())
  }

  override fun mutateResource(resource: Drawable): Drawable {
    return resource.mutate()
  }

  override fun setImage(
    view: ImageView,
    image: Drawable
  ) {
    view.setImageDrawable(image)
  }

}
