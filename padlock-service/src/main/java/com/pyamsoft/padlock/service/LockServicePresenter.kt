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
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.RecheckEvent
import com.pyamsoft.padlock.model.RecheckStatus
import com.pyamsoft.padlock.model.RecheckStatus.NOT_FORCE
import com.pyamsoft.padlock.model.ServiceFinishEvent
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.Presenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class LockServicePresenter @Inject internal constructor(
  private val foregroundEventBus: EventBus<ForegroundEvent>,
  private val serviceFinishBus: EventBus<ServiceFinishEvent>,
  private val recheckEventBus: EventBus<RecheckEvent>,
  private val interactor: LockServiceInteractor
) : Presenter<LockServicePresenter.View>() {

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

    matchingDisposable.dispose()
    entryDisposable.dispose()
  }

  private fun registerForegroundEventListener() {
    dispose {
      foregroundEventBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ interactor.clearMatchingForegroundEvent(it) }, {
            Timber.e(it, "Error listening for foreground event clears")
          })
    }
    dispose {
      interactor.listenForForegroundEvents()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
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
          }, { Timber.e(it, "Error while listening to foreground event") })
    }
  }

  private fun registerOnBus() {
    dispose {
      serviceFinishBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            view?.onFinish()
          }, { Timber.e(it, "onError service finish bus") })
    }

    dispose {
      recheckEventBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            view?.onRecheck(it.packageName, it.className)
          }, { Timber.e(it, "onError recheck event bus") })
    }
  }

  fun processActiveApplicationIfMatching(
    packageName: String,
    className: String
  ) {
    matchingDisposable.dispose()
    matchingDisposable = interactor.isActiveMatching(packageName, className)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
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
    entryDisposable.dispose()
    entryDisposable = interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
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
