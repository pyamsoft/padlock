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

package com.pyamsoft.padlock.service

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.DISABLED
import com.pyamsoft.padlock.api.service.LockServiceInteractor.ServiceState.PERMISSION
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.service.RecheckEvent
import com.pyamsoft.padlock.model.service.RecheckStatus
import com.pyamsoft.padlock.model.service.RecheckStatus.NOT_FORCE
import com.pyamsoft.padlock.model.service.ServiceFinishEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class LockServiceViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val foregroundEventBus: Listener<ForegroundEvent>,
  private val serviceFinishBus: EventBus<ServiceFinishEvent>,
  private val recheckEventBus: Listener<RecheckEvent>,
  private val servicePauseBus: Listener<ServicePauseEvent>,
  private val interactor: LockServiceInteractor
) : BaseViewModel(owner) {

  private val lockScreenBus = RxBus.create<Triple<PadLockEntryModel, String, Int>>()
  private var matchingDisposable by disposable()
  private var entryDisposable by disposable()
  private var foregroundDisposable by disposable()

  init {
    interactor.init()
  }

  override fun onCleared() {
    super.onCleared()
    interactor.cleanup()
    matchingDisposable.tryDispose()
    entryDisposable.tryDispose()
    foregroundDisposable.tryDispose()
  }

  fun onServicePauseEvent(func: (Boolean) -> Unit) {
    dispose {
      servicePauseBus.listen()
          .map { it.autoResume }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  fun onServiceFinishEvent(func: () -> Unit) {
    dispose {
      serviceFinishBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }

    dispose {
      interactor.observeServiceState()
          .filter { it == DISABLED }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onPermissionLostEvent(func: () -> Unit) {
    dispose {
      interactor.observeServiceState()
          .filter { it == PERMISSION }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onLockScreen(func: (PadLockEntryModel, String, Int) -> Unit) {
    dispose {
      lockScreenBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func(it.first, it.second, it.third) }
    }

    listenForRecheckEvent()
    listenForForegroundEvent()
  }

  fun setServicePaused(paused: Boolean) {
    interactor.pauseService(paused)
  }

  private fun listenForRecheckEvent() {
    dispose {
      recheckEventBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { processActiveApplicationIfMatching(it.packageName, it.className) }
    }
  }

  private fun listenForForegroundEvent() {
    dispose {
      foregroundEventBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ interactor.clearMatchingForegroundEvent(it) }, {
            Timber.e(it, "Error listening for foreground event clears")
          })
    }

    dispose {
      interactor.observeScreenState()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            foregroundDisposable.dispose()
            if (it) {
              // Screen on, begin observing foreground
              Timber.d("Screen ON - start observing foreground")
              watchForeground()
            } else {
              Timber.d("Screen OFF - stop observing foreground")
            }
          }
    }
  }

  private fun processActiveApplicationIfMatching(
    packageName: String,
    className: String
  ) {
    matchingDisposable = interactor.ifActiveMatching(packageName, className)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          processEvent(packageName, className, RecheckStatus.FORCE)
        }, { Timber.e(it, "onError processActiveApplicationIfMatching") })
  }

  private fun watchForeground() {
    foregroundDisposable = interactor.listenForForegroundEvents()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnCancel { Timber.d("Cancelling foreground listener") }
        .doAfterTerminate { serviceFinishBus.publish(ServiceFinishEvent) }
        .subscribe({
          if (ForegroundEvent.isEmpty(it)) {
            Timber.w("Ignore empty foreground entry event")
          } else {
            processEvent(it.packageName, it.className, NOT_FORCE)
          }
        }, { Timber.e(it, "Error while listening to foreground event") })
  }

  private fun processEvent(
    packageName: String,
    className: String,
    forcedRecheck: RecheckStatus
  ) {
    entryDisposable = interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ (model, icon) ->
          if (PadLockDbModels.isEmpty(model)) {
            Timber.w("PadLockDbEntryImpl is EMPTY, ignore")
          } else {
            lockScreenBus.publish(Triple(model, className, icon))
          }
        }, {
          if (it is NoSuchElementException) {
            Timber.w("PadLock not locking: $packageName, $className")
          } else {
            Timber.e(it, "Error getting PadLockDbEntryImpl for LockScreen")
          }
        })
  }

}
