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
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.PackageIconManager
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.loader.GenericLoader
import com.pyamsoft.pydroid.loader.cache.ImageCache
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@JvmSuppressWildcards
@Singleton
class AppIconLoader @Inject internal constructor(
    private val appIconImageCache: ImageCache<String, Drawable>,
    @param:Named("main") private val mainScheduler: Scheduler,
    @param:Named("io") private val ioScheduler: Scheduler,
    private val packageIconManager: PackageIconManager<Drawable>
) : Cache {

  override fun clearCache() {
    appIconImageCache.clearCache()
  }

  @CheckResult
  fun forPackageName(packageName: String): GenericLoader<Drawable> =
      AppIconImageLoader(
          packageName, appIconImageCache,
          packageIconManager,
          mainScheduler,
          ioScheduler
      )
}
