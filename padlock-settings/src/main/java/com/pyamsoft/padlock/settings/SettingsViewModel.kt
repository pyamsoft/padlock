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

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.model.ConfirmEvent.ALL
import com.pyamsoft.padlock.model.ConfirmEvent.DATABASE
import com.pyamsoft.padlock.model.pin.ClearPinEvent
import com.pyamsoft.padlock.model.service.ServiceFinishEvent
import com.pyamsoft.pydroid.core.DataBus
import com.pyamsoft.pydroid.core.DataWrapper
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SettingsViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: SettingsInteractor,
  private val bus: EventBus<ConfirmEvent>,
  private val serviceFinishBus: Publisher<ServiceFinishEvent>,
  private val clearPinBus: EventBus<ClearPinEvent>
) {

  private val applicationBus = DataBus<Unit>()
  private val lockTypeBus = DataBus<String>()

  @CheckResult
  fun onDatabaseCleared(func: () -> Unit): Disposable {
    return bus.listen()
        .observeOn(Schedulers.io())
        .filter { it == DATABASE }
        .flatMapSingle {
          enforcer.assertNotOnMainThread()
          return@flatMapSingle interactor.clearDatabase()
              .observeOn(Schedulers.io())
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onAllSettingsCleared(func: () -> Unit): Disposable {
    return bus.listen()
        .observeOn(Schedulers.io())
        .filter { it == ALL }
        .flatMapSingle {
          enforcer.assertNotOnMainThread()
          return@flatMapSingle interactor.clearAll()
              .observeOn(Schedulers.io())
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { serviceFinishBus.publish(ServiceFinishEvent) }
        .subscribe { func() }
  }

  @CheckResult
  fun onPinCleared(func: () -> Unit): Disposable {
    return clearPinBus.listen()
        .filter { it.success }
        .map { Unit }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onPinClearFailed(func: () -> Unit): Disposable {
    return clearPinBus.listen()
        .filter { !it.success }
        .map { Unit }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onApplicationReceiverChanged(func: (DataWrapper<Unit>) -> Unit): Disposable {
    return applicationBus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
  }

  @CheckResult
  fun onLockTypeSwitched(func: (DataWrapper<String>) -> Unit): Disposable {
    return lockTypeBus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(func)
  }

  @CheckResult
  fun updateApplicationReceiver(): Disposable {
    return interactor.updateApplicationReceiver()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { applicationBus.publishLoading(false) }
        .doAfterTerminate { applicationBus.publishComplete() }
        .subscribe({ applicationBus.publishSuccess(Unit) }, {
          Timber.e(it, "Error updating application receiver")
          applicationBus.publishError(it)
        })
  }

  @CheckResult
  fun switchLockType(value: String): Disposable {
    return interactor.hasExistingMasterPassword()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { lockTypeBus.publishLoading(false) }
        .doAfterTerminate { lockTypeBus.publishComplete() }
        .subscribe({ lockTypeBus.publishSuccess(if (it) "" else value) }, {
          Timber.e(it, "Error switching lock type")
          lockTypeBus.publishError(it)
        })
  }
}
