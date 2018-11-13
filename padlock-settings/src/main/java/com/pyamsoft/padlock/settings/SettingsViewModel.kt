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
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.Publisher
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val enforcer: Enforcer,
  private val interactor: SettingsInteractor,
  private val bus: Listener<ConfirmEvent>,
  private val serviceFinishBus: Publisher<ServiceFinishEvent>,
  private val clearPinBus: Listener<ClearPinEvent>,
  @param:Named("recreate_publisher") private val recreatePublisher: Publisher<Unit>
) : BaseViewModel(owner) {

  private val applicationBus = DataBus<Unit>()
  private val lockTypeBus = DataBus<String>()

  private var updateDisposable by singleDisposable()
  private var switchDisposable by singleDisposable()

  override fun onCleared() {
    super.onCleared()
    updateDisposable.tryDispose()
    switchDisposable.tryDispose()
  }

  fun onDatabaseCleared(func: () -> Unit) {
    dispose {
      bus.listen()
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
  }

  fun onAllSettingsCleared(func: () -> Unit) {
    dispose {
      bus.listen()
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
  }

  fun onPinCleared(func: () -> Unit) {
    dispose {
      clearPinBus.listen()
          .filter { it.success }
          .map { Unit }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onPinClearFailed(func: () -> Unit) {
    dispose {
      clearPinBus.listen()
          .filter { !it.success }
          .map { Unit }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onApplicationReceiverChanged(func: (DataWrapper<Unit>) -> Unit) {
    dispose {
      applicationBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onLockTypeSwitched(func: (DataWrapper<String>) -> Unit) {
    dispose {
      lockTypeBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun updateApplicationReceiver() {
    updateDisposable = interactor.updateApplicationReceiver()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { applicationBus.publishLoading(false) }
        .doAfterTerminate { applicationBus.publishComplete() }
        .subscribe({ applicationBus.publishSuccess(Unit) }, {
          Timber.e(it, "Error updating application receiver")
          applicationBus.publishError(it)
        })
  }

  fun switchLockType(value: String) {
    switchDisposable = interactor.hasExistingMasterPassword()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { lockTypeBus.publishLoading(false) }
        .doAfterTerminate { lockTypeBus.publishComplete() }
        .subscribe({ lockTypeBus.publishSuccess(if (it) "" else value) }, {
          Timber.e(it, "Error switching lock type")
          lockTypeBus.publishError(it)
        })
  }

  fun publishRecreate() {
    Timber.d("Publish recreate event")
    recreatePublisher.publish(Unit)
  }
}
