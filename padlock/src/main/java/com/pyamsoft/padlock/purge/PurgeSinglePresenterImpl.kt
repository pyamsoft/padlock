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
import com.pyamsoft.padlock.purge.PurgeSinglePresenterImpl.PurgeSingleEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class PurgeSinglePresenterImpl @Inject internal constructor(
  private val interactor: PurgeInteractor,
  bus: EventBus<PurgeSingleEvent>
) : BasePresenter<PurgeSingleEvent, PurgeSinglePresenter.Callback>(bus),
    PurgeSinglePresenter {

  private var purgeDisposable by singleDisposable()

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onSinglePurged(it.stalePackage) }
        .destroy()
  }

  override fun onUnbind() {
    purgeDisposable.tryDispose()
  }

  override fun purge(stalePackage: String) {
    purgeDisposable = interactor.deleteEntry(stalePackage)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .andThen(Single.just(stalePackage))
        .subscribe({ publish(PurgeSingleEvent(stalePackage)) }, {
          Timber.e(it, "Error attempting purge single: $stalePackage")
          callback.onPurgeSingleError(it)
        })
  }

  internal data class PurgeSingleEvent(val stalePackage: String)

}