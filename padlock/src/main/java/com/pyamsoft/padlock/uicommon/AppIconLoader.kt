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

package com.pyamsoft.padlock.uicommon

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.CheckResult
import android.widget.ImageView
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
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
    private val packageName: String) : GenericLoader<Drawable>() {

  @field:Inject internal lateinit var packageDrawableManager: PackageDrawableManager
  @field:[Inject Named("main")] internal lateinit var mainScheduler: Scheduler
  @field:[Inject Named("io")] internal lateinit var ioScheduler: Scheduler

  init {
    if (packageName.isEmpty()) {
      throw IllegalArgumentException("AppIconLoader packageName must be non-empty")
    }
    Injector.obtain<PadLockComponent>(context.applicationContext).inject(this)

    mainScheduler.enforceMainThread()
    ioScheduler.enforceIo()
  }

  override fun into(imageView: ImageView): Loaded = into(
      DrawableImageTarget.forImageView(imageView))

  override fun into(target: Target<Drawable>): Loaded = load(target, packageName)

  @CheckResult
  private fun load(target: Target<Drawable>, packageName: String): Loaded {
    return RxLoaded(packageDrawableManager.loadDrawableForPackageOrDefault(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .doOnSubscribe { startAction() }
        .subscribe({
          target.loadImage(it)
          completeAction(it)
        },
            {
              Timber.e(it, "Error loading Drawable AppIconLoader for: %s", packageName)
              errorAction(it)
            }))
  }

  companion object {

    @CheckResult
    fun forPackageName(context: Context, packageName: String): GenericLoader<Drawable> =
        AppIconLoader(context, packageName)
  }
}
