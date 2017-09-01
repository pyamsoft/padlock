/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.service

import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.lock.LockPassEvent
import com.pyamsoft.padlock.service.LockServicePresenter.Callback
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockServicePresenter @Inject internal constructor(
    private val lockPassBus: EventBus<LockPassEvent>,
    private val serviceFinishBus: EventBus<ServiceFinishEvent>,
    private val recheckEventBus: EventBus<RecheckEvent>,
    private val interactor: LockServiceInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(compScheduler,
    ioScheduler,
    mainScheduler) {

  init {
    interactor.reset()
  }

  override fun onBind(v: Callback) {
    super.onBind(v)
    registerOnBus(v::onRecheck, v::onFinish)
  }

  override fun onUnbind() {
    super.onUnbind()
    interactor.cleanup()
    interactor.reset()
  }

  private fun registerOnBus(onRecheck: (String, String) -> Unit, onFinish: () -> Unit) {
    dispose {
      serviceFinishBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            onFinish()
          }, { Timber.e(it, "onError service finish bus") })
    }

    dispose {
      lockPassBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            interactor.setLockScreenPassed(it.packageName, it.className, true)
          }, { Timber.e(it, "onError lock passed bus") })
    }

    dispose {
      recheckEventBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            onRecheck(it.packageName(), it.className())
          }, { Timber.e(it, "onError recheck event bus") })
    }
  }

  fun processActiveApplicationIfMatching(packageName: String, className: String,
      startLockScreen: (PadLockEntry, String) -> Unit) {
    dispose {
      interactor.isActiveMatching(packageName, className)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              processAccessibilityEvent(packageName, className, RecheckStatus.FORCE,
                  startLockScreen)
            }
          }, { Timber.e(it, "onError processActiveApplicationIfMatching") })
    }
  }

  fun processAccessibilityEvent(packageName: String, className: String,
      forcedRecheck: RecheckStatus, startLockScreen: (PadLockEntry, String) -> Unit) {
    dispose {
      interactor.processEvent(packageName, className, forcedRecheck)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (PadLockEntry.isEmpty(it)) {
              Timber.w("PadLockEntry is EMPTY, ignore")
            } else {
              startLockScreen(it, className)
            }
          }, {
            if (it is NoSuchElementException) {
              Timber.w("PadLock not locking: $packageName, $className")
            } else {
              Timber.e(it, "Error getting PadLockEntry for LockScreen")
            }
          })
    }
  }

  interface Callback {

    fun onFinish()

    fun onRecheck(packageName: String, className: String)
  }
}
