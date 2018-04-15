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

package com.pyamsoft.padlock.base

import android.graphics.drawable.Drawable
import androidx.core.util.lruCache
import com.pyamsoft.pydroid.loader.cache.ImageCache
import com.pyamsoft.pydroid.loader.cache.ImageCache.ImageCacheKey
import javax.inject.Inject
import javax.inject.Singleton

@JvmSuppressWildcards
@Singleton
internal class AppIconImageCache @Inject internal constructor() :
    ImageCache<String, Drawable> {

  private val cache = lruCache<String, Drawable>(8)

  override fun clearCache() {
    cache.evictAll()
  }

  override fun cache(
    key: ImageCacheKey<String>,
    entry: Drawable
  ) {
    cache.put(key.data, entry)
  }

  override fun retrieve(key: ImageCacheKey<String>): Drawable? = cache[key.data]
}
