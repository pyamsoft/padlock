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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.PurgeInteractor
import com.pyamsoft.padlock.purge.PurgeAllPresenterImpl.PurgeAllEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class PurgeAllPresenterImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: PurgeInteractor,
  bus: EventBus<PurgeAllEvent>
) : BasePresenter<PurgeAllEvent, PurgeAllPresenter.Callback>(bus),
    PurgeAllPresenter {

  @CheckResult
  private fun purgeAll(stalePackages: List<String>): Single<List<String>> {
    return Completable.defer {
      enforcer.assertNotOnMainThread()

      return@defer interactor.deleteEntries(stalePackages)
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.io())
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .andThen(Single.just(stalePackages))
  }

  override fun onBind() {
    listen()
        .flatMapSingle { purgeAll(it.stalePackages) }
        .subscribeOn(Schedulers.trampoline())
        .observeOn(Schedulers.trampoline())
        .subscribe { callback.onAllPurged(it) }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun purge(stalePackages: List<String>) {
    publish(PurgeAllEvent(stalePackages))
  }

  internal data class PurgeAllEvent(val stalePackages: List<String>)

}