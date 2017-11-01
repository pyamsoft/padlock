/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.base.loader

import android.graphics.drawable.Drawable
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.wrapper.PackageDrawableManager
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
    private val packageDrawableManager: PackageDrawableManager) : Cache {

  override fun clearCache() {
    appIconImageCache.clearCache()
  }

  @CheckResult
  fun forPackageName(packageName: String): GenericLoader<Drawable> =
      AppIconImageLoader(packageName, appIconImageCache, packageDrawableManager, mainScheduler,
          ioScheduler)
}
