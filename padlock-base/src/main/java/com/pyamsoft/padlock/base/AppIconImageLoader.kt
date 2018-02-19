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
import android.widget.ImageView
import com.pyamsoft.padlock.api.PackageDrawableManager
import com.pyamsoft.pydroid.data.enforceIo
import com.pyamsoft.pydroid.data.enforceMainThread
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
internal class AppIconImageLoader internal constructor(
    private val packageName: String,
    private val appIconImageCache: ImageCache<String, Drawable>,
    private val packageDrawableManager: PackageDrawableManager,
    private val mainScheduler: Scheduler,
    private val ioScheduler: Scheduler
) : GenericLoader<Drawable>() {

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
      DrawableImageTarget.forImageView(imageView)
  )

  override fun into(target: Target<Drawable>): Loaded = load(target, packageName)

  @CheckResult
  private fun load(
      target: Target<Drawable>,
      packageName: String
  ): Loaded {
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
        })
    )
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
