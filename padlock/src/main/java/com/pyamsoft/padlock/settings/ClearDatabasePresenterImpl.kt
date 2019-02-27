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

package com.pyamsoft.padlock.settings

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.settings.ClearDatabasePresenterImpl.ClearDatabaseEvent
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.arch.destroy
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class ClearDatabasePresenterImpl @Inject internal constructor(
  private val interactor: SettingsInteractor,
  bus: EventBus<ClearDatabaseEvent>
) : BasePresenter<ClearDatabaseEvent, ClearDatabasePresenter.Callback>(bus),
    ClearDatabasePresenter {

  @CheckResult
  private fun clearDatabase(): Single<Unit> {
    return interactor.clearDatabase()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  override fun onBind() {
    listen().flatMapSingle { clearDatabase() }
        .subscribeOn(Schedulers.trampoline())
        .observeOn(Schedulers.trampoline())
        .subscribe { callback.onDatabaseCleared() }
        .destroy(owner)
  }

  override fun onUnbind() {
  }

  override fun clear() {
    publish(ClearDatabaseEvent)
  }

  internal object ClearDatabaseEvent

}

