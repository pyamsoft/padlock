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
import android.widget.ImageView
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.packagemanager.PackageIconManager
import com.pyamsoft.padlock.model.IconHolder
import com.pyamsoft.pydroid.loader.GenericLoader
import com.pyamsoft.pydroid.loader.cache.ImageCache
import com.pyamsoft.pydroid.loader.cache.ImageCache.ImageCacheKey
import com.pyamsoft.pydroid.loader.loaded.Loaded
import com.pyamsoft.pydroid.loader.loaded.RxLoaded
import com.pyamsoft.pydroid.loader.targets.DrawableImageTarget
import com.pyamsoft.pydroid.loader.targets.Target
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

@JvmSuppressWildcards
internal class AppIconImageLoader internal constructor(
  private val packageName: String,
  private val appIconImageCache: ImageCache<String, Drawable>,
  private val packageIconManager: PackageIconManager<Drawable>
) : GenericLoader<Drawable>() {

  @CheckResult
  private fun String.toKey(): ImageCacheKey<String> = ImageCacheKey(this)

  init {
    if (packageName.isEmpty()) {
      throw IllegalArgumentException("AppIconLoader packageName must be non-empty")
    }
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
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { startAction?.invoke() }
        .subscribe({ holder ->
          holder.applyIcon {
            target.loadImage(it)
            completeAction?.invoke(it)
          }
        }, {
          Timber.e(it, "Error loading Drawable AppIconLoader for: %s", packageName)
          errorAction?.invoke(it)
        })
    )
  }

  @CheckResult
  private fun loadCached(packageName: String): Single<IconHolder<Drawable>> {
    return Single.defer {
      val key: ImageCacheKey<String> = packageName.toKey()
      val cached: Drawable? = appIconImageCache.retrieve(key)
      if (cached == null) {
        val result = loadFresh(packageName)
        return@defer result.doOnSuccess { holder ->
          holder.applyIcon {
            appIconImageCache.cache(key, it)
          }
        }
      } else {
        return@defer Single.just(IconHolder(cached))
      }
    }
  }

  @CheckResult
  private fun loadFresh(packageName: String): Single<IconHolder<Drawable>> =
    packageIconManager.loadIconForPackageOrDefault(packageName)
}
