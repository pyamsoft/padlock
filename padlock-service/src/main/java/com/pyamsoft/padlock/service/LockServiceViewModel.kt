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

package com.pyamsoft.padlock.service

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.service.LockServiceInteractor
import com.pyamsoft.padlock.model.ForegroundEvent
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.service.RecheckStatus
import com.pyamsoft.padlock.model.service.RecheckStatus.FORCE
import com.pyamsoft.padlock.model.service.ServicePauseState
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class LockServiceViewModel @Inject internal constructor(
  private val foregroundEventBus: Listener<ForegroundEvent>,
  private val interactor: LockServiceInteractor
) {

  init {
    interactor.init()
  }

  @CheckResult
  fun observeScreenState(
    onScreenOn: () -> Unit,
    onScreenOff: () -> Unit
  ): Disposable {
    return interactor.observeScreenState()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { screenOn: Boolean ->
          if (screenOn) {
            onScreenOn()
          } else {
            onScreenOff()
          }
        }
  }

  @CheckResult
  fun onForegroundApplicationLockRequest(
    onEvent: (model: PadLockEntryModel, className: String, icon: Int) -> Unit,
    onError: (error: Throwable) -> Unit
  ): Disposable {
    val foregroundDisposable = foregroundEventBus.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ interactor.clearMatchingForegroundEvent(it) }, {
          Timber.e(it, "Error listening for foreground event clears")
        })

//    val eventDisposable = interactor.listenForForegroundEvents()
//        .filter { !ForegroundEvent.isEmpty(it) }
//        .subscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .doOnCancel { Timber.d("Cancelling foreground listener") }
//        .doAfterTerminate { serviceFinishBus.publish(ServiceFinishEvent) }
//        .flatMapMaybe { processEvent(it.packageName, it.className, NOT_FORCE) }
//        .subscribe({ (model: PadLockEntryModel, className: String, icon: Int) ->
//          onEvent(model, className, icon)
//        }, {
//          Timber.e(it, "Error while watching foreground events")
//          onError(it)
//        })

    return object : Disposable {

      override fun isDisposed(): Boolean {
        return foregroundDisposable.isDisposed
      }

      override fun dispose() {
        foregroundDisposable.tryDispose()
      }

    }
  }

  @CheckResult
  fun onRecheckForcedLockEvent(
    onEvent: (model: PadLockEntryModel, className: String, icon: Int) -> Unit,
    onError: (error: Throwable) -> Unit
  ): Disposable {
    return Disposables.empty()
//    return recheckEventBus.listen()
//        .flatMapMaybe { processActiveApplicationIfMatching(it.packageName, it.className) }
//        .subscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe({ (model: PadLockEntryModel, className: String, icon: Int) ->
//          onEvent(model, className, icon)
//        }, {
//          Timber.e(it, "Error while watching foreground events")
//          onError(it)
//        })
  }

  @CheckResult
  private fun processActiveApplicationIfMatching(
    packageName: String,
    className: String
  ): Maybe<Triple<PadLockEntryModel, String, Int>> {
    return interactor.ifActiveMatching(packageName, className)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap { processEvent(packageName, className, FORCE) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }

  @CheckResult
  private fun processEvent(
    packageName: String,
    className: String,
    forcedRecheck: RecheckStatus
  ): Maybe<Triple<PadLockEntryModel, String, Int>> {
    return interactor.processEvent(packageName, className, forcedRecheck)
        .filter { (model, _) -> !PadLockDbModels.isEmpty(model) }
        .filter { (_, icon) -> icon != 0 }
        .map { Triple(it.first, className, it.second) }
        .unsubscribeOn(Schedulers.io())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }

  fun setServicePaused(paused: ServicePauseState) {
    interactor.pauseService(paused)
  }

}
