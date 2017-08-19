/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.uicommon

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.CheckResult
import android.widget.ImageView
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.base.wrapper.PackageDrawableManager
import com.pyamsoft.pydroid.helper.enforceIo
import com.pyamsoft.pydroid.helper.enforceMainThread
import com.pyamsoft.pydroid.loader.GenericLoader
import com.pyamsoft.pydroid.loader.loaded.Loaded
import com.pyamsoft.pydroid.loader.loaded.RxLoaded
import com.pyamsoft.pydroid.loader.targets.DrawableImageTarget
import com.pyamsoft.pydroid.loader.targets.Target
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class AppIconLoader private constructor(context: Context,
    private val packageName: String) : GenericLoader<AppIconLoader, Drawable>() {

  @Suppress("MemberVisibilityCanPrivate")
  @field:Inject internal lateinit var packageDrawableManager: PackageDrawableManager
  @Suppress("MemberVisibilityCanPrivate")
  @field:[Inject Named("main")] internal lateinit var mainScheduler: Scheduler
  @Suppress("MemberVisibilityCanPrivate")
  @field:[Inject Named("io")] internal lateinit var ioScheduler: Scheduler

  init {
    if (this.packageName.isEmpty()) {
      throw IllegalArgumentException("AppIconLoader packageName must be non-empty")
    }

    Injector.with(context) {
      it.inject(this)
    }

    mainScheduler.enforceMainThread()
    ioScheduler.enforceIo()
  }

  override fun withCompleteAction(completeAction: (Target<Drawable>) -> Unit): AppIconLoader {
    this.completeAction = completeAction
    return this
  }

  override fun withErrorAction(errorAction: (Target<Drawable>) -> Unit): AppIconLoader {
    this.errorAction = errorAction
    return this
  }

  override fun withStartAction(startAction: (Target<Drawable>) -> Unit): AppIconLoader {
    this.startAction = startAction
    return this
  }


  override fun into(imageView: ImageView): Loaded = into(
      DrawableImageTarget.forImageView(imageView))

  override fun into(target: Target<Drawable>): Loaded = load(target, packageName)

  @CheckResult
  private fun load(target: Target<Drawable>, packageName: String): Loaded {
    return RxLoaded(packageDrawableManager.loadDrawableForPackageOrDefault(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe({ target.loadImage(it) }
        ) { throwable ->
          Timber.e(throwable, "Error loading Drawable AppIconLoader for: %s",
              packageName)
        })
  }

  override fun tint(color: Int): AppIconLoader {
    this.tint = tint
    return this
  }

  companion object {

    @CheckResult
    @JvmStatic
    fun forPackageName(context: Context, packageName: String): AppIconLoader =
        AppIconLoader(context, packageName)
  }
}
