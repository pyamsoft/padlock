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
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: SettingsInteractor,
  private val bus: Listener<ConfirmEvent>,
  private val serviceFinishBus: Publisher<ServiceFinishEvent>,
  private val clearPinBus: Listener<ClearPinEvent>,
  @param:Named("recreate_publisher") private val recreatePublisher: Publisher<Unit>
) {

  @CheckResult
  private fun clearDatabase(): Single<ConfirmEvent> {
    enforcer.assertNotOnMainThread()
    return interactor.clearDatabase()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  private fun clearAll(): Single<ConfirmEvent> {
    enforcer.assertNotOnMainThread()
    return interactor.clearAll()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  fun onDatabaseCleared(func: () -> Unit): Disposable {
    return bus.listen()
        .observeOn(Schedulers.io())
        .filter { it == DATABASE }
        .flatMapSingle { clearDatabase() }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onAllSettingsCleared(func: () -> Unit): Disposable {
    return bus.listen()
        .observeOn(Schedulers.io())
        .filter { it == ALL }
        .flatMapSingle { clearAll() }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { serviceFinishBus.publish(ServiceFinishEvent) }
        .subscribe { func() }
  }

  @CheckResult
  fun onPinClearSuccess(func: () -> Unit): Disposable {
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
  fun updateApplicationReceiver(
    onUpdateBegin: () -> Unit,
    onUpdateSuccess: () -> Unit,
    onUpdateError: (error: Throwable) -> Unit,
    onUpdateComplete: () -> Unit
  ): Disposable {
    return interactor.updateApplicationReceiver()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { onUpdateBegin() }
        .doAfterTerminate { onUpdateComplete() }
        .subscribe({ onUpdateSuccess() }, {
          Timber.e(it, "Error updating application receiver")
          onUpdateError(it)
        })
  }

  @CheckResult
  fun switchLockType(
    onSwitchBegin: () -> Unit,
    onSwitchSuccess: (canSwitch: Boolean) -> Unit,
    onSwitchError: (error: Throwable) -> Unit,
    onSwitchComplete: () -> Unit
  ): Disposable {
    return interactor.hasExistingMasterPassword()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { onSwitchBegin() }
        .doAfterTerminate { onSwitchComplete() }
        .subscribe({ onSwitchSuccess(!it) }, {
          Timber.e(it, "Error switching lock type")
          onSwitchError(it)
        })
  }

  fun publishRecreate() {
    Timber.d("Publish recreate event")
    recreatePublisher.publish(Unit)
  }
}
