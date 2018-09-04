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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.popinnow.android.repo.SingleRepo
import com.pyamsoft.padlock.api.packagemanager.PackageIconManager
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.loader.GenericLoader
import com.pyamsoft.pydroid.loader.Loaded
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

@JvmSuppressWildcards
internal class AppIconImageLoader internal constructor(
  private val enforcer: Enforcer,
  private val packageName: String,
  private val packageIconManager: PackageIconManager,
  private val cache: SingleRepo<Drawable>
) : GenericLoader<Drawable>() {

  init {
    if (packageName.isEmpty()) {
      throw IllegalArgumentException("AppIconLoader packageName must be non-empty")
    }
  }

  override fun into(imageView: ImageView): Loaded {
    return RxLoaded(
        load(packageName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
              Timber.d("Loaded App icon for $packageName")
              imageView.setImageDrawable(it)
            }, {
              Timber.e(it, "Error loading App icon for $packageName")
              imageView.setImageDrawable(null)
            }), imageView
    )
  }

  @CheckResult
  private fun load(packageName: String): Single<Drawable> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer cache.get(false, packageName) {
      enforcer.assertNotOnMainThread()
      return@get packageIconManager.loadIcon(it)
    }
  }

  private data class RxLoaded(
    private val disposable: Disposable,
    private val imageView: ImageView
  ) : Loaded, LifecycleObserver {

    private var lifecycle: Lifecycle? = null

    override fun bind(owner: LifecycleOwner) {
      owner.lifecycle.addObserver(this)
      lifecycle = owner.lifecycle
    }

    @OnLifecycleEvent(ON_DESTROY)
    internal fun onDestroy() {
      lifecycle?.removeObserver(this)
      lifecycle = null

      if (!disposable.isDisposed) {
        disposable.dispose()
        imageView.setImageDrawable(null)
      }
    }

  }
}
