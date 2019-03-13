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
import com.pyamsoft.padlock.purge.PurgePresenterImpl.PurgeEvent
import com.pyamsoft.padlock.purge.PurgePresenterImpl.PurgeEvent.Begin
import com.pyamsoft.padlock.purge.PurgePresenterImpl.PurgeEvent.Complete
import com.pyamsoft.padlock.purge.PurgePresenterImpl.PurgeEvent.Error
import com.pyamsoft.padlock.purge.PurgePresenterImpl.PurgeEvent.Success
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class PurgePresenterImpl @Inject internal constructor(
  private val interactor: PurgeInteractor,
  bus: EventBus<PurgeEvent>
) : BasePresenter<PurgeEvent, PurgePresenter.Callback>(bus),
    PurgePresenter {

  private var fetchDisposable by singleDisposable()

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          return@subscribe when (it) {
            is Begin -> callback.onFetchStaleBegin()
            is Success -> callback.onFetchStaleSuccess(it.data)
            is Error -> callback.onFetchStaleError(it.throwable)
            is Complete -> callback.onFetchStaleComplete()
          }
        }
        .destroy()
  }

  override fun onUnbind() {
    fetchDisposable.tryDispose()
  }

  override fun fetchData(forced: Boolean) {
    fetchDisposable = interactor.fetchStalePackageNames(forced)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { publish(Complete) }
        .doOnSubscribe { publish(Begin) }
        .subscribe({ publish(Success(it)) }, {
          Timber.e(it, "Error fetching stale applications")
          publish(Error(it))
        })
  }

  internal sealed class PurgeEvent {

    object Begin : PurgeEvent()

    data class Success(val data: List<String>) : PurgeEvent()

    data class Error(val throwable: Throwable) : PurgeEvent()

    object Complete : PurgeEvent()
  }
}