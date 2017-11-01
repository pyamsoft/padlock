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
import android.widget.ImageView
import com.pyamsoft.padlock.base.wrapper.PackageDrawableManager
import com.pyamsoft.pydroid.helper.enforceIo
import com.pyamsoft.pydroid.helper.enforceMainThread
import com.pyamsoft.pydroid.loader.GenericLoader
import com.pyamsoft.pydroid.loader.cache.ImageCache
import com.pyamsoft.pydroid.loader.cache.ImageCache.ImageCacheKey
import com.pyamsoft.pydroid.loader.loaded.Loaded
import com.pyamsoft.pydroid.loader.loaded.RxLoaded
import com.pyamsoft.pydroid.loader.targets.DrawableImageTarget
import com.pyamsoft.pydroid.loader.targets.Target
import io.reactivex.Scheduler
import io.reactivex.Single
import timber.log.Timber

@JvmSuppressWildcards
internal class AppIconImageLoader internal constructor(private val packageName: String,
    private val appIconImageCache: ImageCache<String, Drawable>,
    private val packageDrawableManager: PackageDrawableManager,
    private val mainScheduler: Scheduler,
    private val ioScheduler: Scheduler) : GenericLoader<Drawable>() {

  @CheckResult
  private fun String.toKey(): ImageCacheKey<String> = ImageCacheKey(this)

  init {
    if (packageName.isEmpty()) {
      throw IllegalArgumentException("AppIconLoader packageName must be non-empty")
    }

    mainScheduler.enforceMainThread()
    ioScheduler.enforceIo()
  }

  override fun into(imageView: ImageView): Loaded = into(
      DrawableImageTarget.forImageView(imageView))

  override fun into(target: Target<Drawable>): Loaded = load(target, packageName)

  @CheckResult
  private fun load(target: Target<Drawable>, packageName: String): Loaded {
    return RxLoaded(loadCached(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .doOnSubscribe { startAction?.invoke() }
        .subscribe({
          target.loadImage(it)
          completeAction?.invoke(it)
        }, {
          Timber.e(it, "Error loading Drawable AppIconLoader for: %s", packageName)
          errorAction?.invoke(it)
        }))
  }

  @CheckResult
  private fun loadCached(packageName: String): Single<Drawable> {
    val key: ImageCacheKey<String> = packageName.toKey()
    val cached: Drawable? = appIconImageCache.retrieve(key)
    if (cached == null) {
      val result = loadFresh(packageName)
      return result.doOnSuccess {
        appIconImageCache.cache(key, it)
      }
    } else {
      return Single.just(cached)
    }
  }

  @CheckResult
  private fun loadFresh(packageName: String): Single<Drawable> =
      packageDrawableManager.loadDrawableForPackageOrDefault(packageName)
}