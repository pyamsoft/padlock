/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.purge

import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.purge.PurgeAllPresenterImpl.PurgeAllEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class PurgeAllPresenterImpl @Inject internal constructor(
  private val interactor: PurgeInteractor,
  bus: EventBus<PurgeAllEvent>
) : BasePresenter<PurgeAllEvent, PurgeAllPresenter.Callback>(bus),
    PurgeAllPresenter {

  private var purgeDisposable by singleDisposable()

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onAllPurged(it.stalePackages) }
        .destroy()
  }

  override fun onUnbind() {
    purgeDisposable.tryDispose()
  }

  override fun purge(stalePackages: List<String>) {
    purgeDisposable = interactor.deleteEntries(stalePackages)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .andThen(Single.just(stalePackages))
        .subscribe({ publish(PurgeAllEvent(stalePackages)) }, {
          Timber.e(it, "Error attempting purge all: $stalePackages")
          callback.onPurgeAllError(it)
        })
  }

  internal data class PurgeAllEvent(val stalePackages: List<String>)

}