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

package com.pyamsoft.padlock.settings

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.model.ConfirmEvent.ALL
import com.pyamsoft.padlock.model.ConfirmEvent.DATABASE
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.service.ServiceFinishEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper
import com.pyamsoft.pydroid.core.viewmodel.LifecycleViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SettingsViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: SettingsInteractor,
  private val bus: EventBus<ConfirmEvent>,
  private val serviceFinishBus: EventBus<ServiceFinishEvent>,
  private val clearPinBus: EventBus<ClearPinEvent>
) : LifecycleViewModel {

  private val applicationBus = DataBus<Unit>()
  private val lockTypeBus = DataBus<String>()

  fun onDatabaseCleared(
    owner: LifecycleOwner,
    func: () -> Unit
  ) {
    bus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .filter { it == DATABASE }
        .flatMapSingle {
          enforcer.assertNotOnMainThread()
          return@flatMapSingle interactor.clearDatabase()
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
        .bind(owner)
  }

  fun onAllSettingsCleared(
    owner: LifecycleOwner,
    func: () -> Unit
  ) {
    bus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .filter { it == ALL }
        .flatMapSingle {
          enforcer.assertNotOnMainThread()
          return@flatMapSingle interactor.clearAll()
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { serviceFinishBus.publish(ServiceFinishEvent) }
        .subscribe { func() }
        .bind(owner)
  }

  fun onPinCleared(
    owner: LifecycleOwner,
    func: () -> Unit
  ) {
    clearPinBus.listen()
        .filter { it.success }
        .map { Unit }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
        .bind(owner)
  }

  fun onPinClearFailed(
    owner: LifecycleOwner,
    func: () -> Unit
  ) {
    clearPinBus.listen()
        .filter { !it.success }
        .map { Unit }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
        .bind(owner)
  }

  fun onApplicationReceiverChanged(
    owner: LifecycleOwner,
    func: (DataWrapper<Unit>) -> Unit
  ) {
    applicationBus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
        .bind(owner)
  }

  fun onLockTypeSwitched(
    owner: LifecycleOwner,
    func: (DataWrapper<String>) -> Unit
  ) {
    lockTypeBus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
        .bind(owner)
  }

  fun updateApplicationReceiver(owner: LifecycleOwner) {
    interactor.updateApplicationReceiver()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { applicationBus.publishLoading(false) }
        .doAfterTerminate { applicationBus.publishComplete() }
        .subscribe({ applicationBus.publishSuccess(Unit) }, {
          Timber.e(it, "Error updating application receiver")
          applicationBus.publishError(it)
        })
        .disposeOnClear(owner)
  }

  fun switchLockType(
    owner: LifecycleOwner,
    value: String
  ) {
    interactor.hasExistingMasterPassword()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { lockTypeBus.publishLoading(false) }
        .doAfterTerminate { lockTypeBus.publishComplete() }
        .subscribe({ lockTypeBus.publishSuccess(if (it) "" else value) }, {
          Timber.e(it, "Error switching lock type")
          lockTypeBus.publishError(it)
        })
        .disposeOnClear(owner)
  }
}
