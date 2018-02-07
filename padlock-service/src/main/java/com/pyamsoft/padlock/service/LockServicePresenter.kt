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

import com.pyamsoft.padlock.api.LockServiceInteractor
import com.pyamsoft.padlock.model.*
import com.pyamsoft.padlock.model.RecheckStatus.NOT_FORCE
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.service.LockServicePresenter.View
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.ktext.clear
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockServicePresenter @Inject internal constructor(
    private val foregroundEventBus: EventBus<ForegroundEvent>,
    private val serviceFinishBus: EventBus<ServiceFinishEvent>,
    private val recheckEventBus: EventBus<RecheckEvent>,
    private val interactor: LockServiceInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler
) : SchedulerPresenter<View>(
    compScheduler,
    ioScheduler, mainScheduler
) {

  private var matchingDisposable: Disposable = Disposables.empty()
  private var entryDisposable: Disposable = Disposables.empty()

  init {
    interactor.reset()
  }

  override fun onCreate() {
    super.onCreate()
    registerOnBus()
    registerForegroundEventListener()
  }

  override fun onDestroy() {
    super.onDestroy()
    interactor.cleanup()
    interactor.reset()

    matchingDisposable = matchingDisposable.clear()
    entryDisposable = entryDisposable.clear()
  }

  private fun registerForegroundEventListener() {
    dispose {
      foregroundEventBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({ interactor.clearMatchingForegroundEvent(it) }, {
            Timber.e(it, "Error listening for foreground event clears")
          })
    }
    dispose {
      interactor.listenForForegroundEvents()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .onErrorReturn {
            Timber.e(it, "Error while listening to foreground events")
            return@onErrorReturn ForegroundEvent.EMPTY
          }
          .doAfterTerminate { view?.onFinish() }
          .subscribe({
            if (ForegroundEvent.isEmpty(it)) {
              Timber.w("Ignore empty foreground entry event")
            } else {
              processEvent(it.packageName, it.className, NOT_FORCE)
            }
          },
              {
                Timber.e(
                    it,
                    "Error while listening to foreground event, killing stream"
                )
              },
              { Timber.d("Foreground event stream completed") })
    }
  }

  private fun registerOnBus() {
    dispose {
      serviceFinishBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            view?.onFinish()
          }, { Timber.e(it, "onError service finish bus") })
    }

    dispose {
      recheckEventBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            view?.onRecheck(it.packageName, it.className)
          }, { Timber.e(it, "onError recheck event bus") })
    }
  }

  fun processActiveApplicationIfMatching(
      packageName: String,
      className: String
  ) {
    matchingDisposable = matchingDisposable.clear()
    matchingDisposable = interactor.isActiveMatching(packageName, className)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({
          if (it) {
            processEvent(packageName, className, RecheckStatus.FORCE)
          }
        }, { Timber.e(it, "onError processActiveApplicationIfMatching") })
  }

  private fun processEvent(
      packageName: String,
      className: String,
      forcedRecheck: RecheckStatus
  ) {
    entryDisposable = entryDisposable.clear()
    entryDisposable = interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({
          if (PadLockEntry.isEmpty(it)) {
            Timber.w("PadLockEntry is EMPTY, ignore")
          } else {
            view?.onStartLockScreen(it, className)
          }
        }, {
          if (it is NoSuchElementException) {
            Timber.w("PadLock not locking: $packageName, $className")
          } else {
            Timber.e(it, "Error getting PadLockEntry for LockScreen")
          }
        })
  }

  interface View : ForegroundEventStreamCallback, BusCallback, LockScreenCallback

  interface ForegroundEventStreamCallback {

    fun onFinish()
  }

  interface LockScreenCallback {

    fun onStartLockScreen(
        entry: PadLockEntryModel,
        realName: String
    )
  }

  interface BusCallback : ForegroundEventStreamCallback {

    fun onRecheck(
        packageName: String,
        className: String
    )
  }
}
