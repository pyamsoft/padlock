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

package com.pyamsoft.padlock.settings

import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver
import com.pyamsoft.padlock.settings.ConfirmEvent.Type
import com.pyamsoft.padlock.settings.ConfirmEvent.Type.ALL
import com.pyamsoft.padlock.settings.ConfirmEvent.Type.DATABASE
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SettingsPreferencePresenter @Inject internal constructor(
    private val interactor: SettingsPreferenceInteractor,
    private val bus: ConfirmEventBus,
    private val receiver: ApplicationInstallReceiver, @Named("obs") obsScheduler: Scheduler,
    @Named("sub") subScheduler: Scheduler) : SchedulerPresenter(obsScheduler, subScheduler) {

  fun setApplicationInstallReceiverState() {
    disposeOnStop(interactor.isInstallListenerEnabled
        .subscribeOn(backgroundScheduler)
        .observeOn(foregroundScheduler)
        .subscribe({
          if (it) {
            receiver.register()
          } else {
            receiver.unregister()
          }
        }, { Timber.e(it, "onError setApplicationInstallReceiverState") }))
  }

  fun registerOnBus(onClearDatabase: () -> Unit, onClearAll: () -> Unit) {
    disposeOnStop {
      bus.listen().flatMapSingle {
        val result: Single<Type>
        when (it.type()) {
          DATABASE -> result = interactor.clearDatabase().map { Type.DATABASE }
          ALL -> result = interactor.clearAll().map { Type.ALL }
        }

        return@flatMapSingle result
      }.subscribeOn(backgroundScheduler).observeOn(foregroundScheduler)
          .subscribe({
            when (it) {
              DATABASE -> onClearDatabase()
              ALL -> onClearAll()
            }
          }, {
            Timber.e(it, "onError clear bus")
          })
    }
  }

  fun checkLockType(onBegin: () -> Unit, onLockTypeChangeAccepted: () -> Unit,
      onLockTypeChangePrevented: () -> Unit, onLockTypeChangeError: (Throwable) -> Unit,
      onEnd: () -> Unit) {
    disposeOnStop(interactor.hasExistingMasterPassword()
        .subscribeOn(backgroundScheduler)
        .observeOn(foregroundScheduler)
        .doAfterTerminate { onEnd() }
        .doOnSubscribe { onBegin() }
        .subscribe({ hasMasterPin ->
          if (hasMasterPin) {
            onLockTypeChangePrevented()
          } else {
            onLockTypeChangeAccepted()
          }
        }, { throwable ->
          Timber.e(throwable, "on error lock type change")
          onLockTypeChangeError(throwable)
        }))
  }
}
