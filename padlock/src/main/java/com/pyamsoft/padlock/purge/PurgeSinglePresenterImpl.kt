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
import com.pyamsoft.padlock.purge.PurgeSinglePresenterImpl.PurgeSingleEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class PurgeSinglePresenterImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: PurgeInteractor,
  bus: EventBus<PurgeSingleEvent>
) : BasePresenter<PurgeSingleEvent, PurgeSinglePresenter.Callback>(bus),
    PurgeSinglePresenter {

  @CheckResult
  private fun purgeSingle(stalePackage: String): Single<String> {
    enforcer.assertNotOnMainThread()
    return interactor.deleteEntry(stalePackage)
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .andThen(Single.just(stalePackage))
  }

  override fun onBind() {
    listen()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMapSingle { purgeSingle(it.stalePackage) }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onSinglePurged(it) }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun purge(stalePackage: String) {
    publish(PurgeSingleEvent(stalePackage))
  }

  internal data class PurgeSingleEvent(val stalePackage: String)

}