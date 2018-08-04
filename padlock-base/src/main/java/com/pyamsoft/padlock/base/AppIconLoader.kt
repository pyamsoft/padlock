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
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.packagemanager.PackageIconManager
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.loader.GenericLoader
import com.pyamsoft.pydroid.loader.cache.ImageCache
import javax.inject.Inject
import javax.inject.Singleton

@JvmSuppressWildcards
@Singleton
class AppIconLoader @Inject internal constructor(
  private val appIconImageCache: ImageCache<String, Drawable>,
  private val packageIconManager: PackageIconManager<Drawable>
) : Cache {

  override fun clearCache() {
    appIconImageCache.clearCache()
  }

  @CheckResult
  fun forPackageName(packageName: String): GenericLoader<Drawable> =
    AppIconImageLoader(packageName, appIconImageCache, packageIconManager)
}
