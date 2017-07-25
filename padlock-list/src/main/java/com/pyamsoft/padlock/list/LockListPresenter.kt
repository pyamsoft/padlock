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

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.pin.ClearPinBus
import com.pyamsoft.padlock.pin.CreatePinBus
import com.pyamsoft.padlock.service.LockServiceStateInteractor
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class LockListPresenter @Inject constructor(
    private val lockListInteractor: LockListInteractor,
    private val stateInteractor: LockServiceStateInteractor,
    private val clearPinBus: ClearPinBus,
    private val createPinBus: CreatePinBus,
    @Named("obs") obsScheduler: Scheduler,
    @Named("io") subScheduler: Scheduler) : SchedulerPresenter(obsScheduler, subScheduler) {

  fun registerOnBus(onMasterPinCreateSuccess: () -> Unit, onMasterPinCreateFailure: () -> Unit,
      onMasterPinClearSuccess: () -> Unit, onMasterPinClearFailure: () -> Unit) {
    disposeOnStop {
      createPinBus.listen().subscribeOn(backgroundScheduler).observeOn(foregroundScheduler)
          .subscribe({
            if (it.success()) {
              onMasterPinCreateSuccess()
            } else {
              onMasterPinCreateFailure()
            }
          }, {
            Timber.e(it, "error create pin bus")
          })
    }

    disposeOnStop {
      clearPinBus.listen().subscribeOn(backgroundScheduler).observeOn(foregroundScheduler)
          .subscribe({
            if (it.success()) {
              onMasterPinClearSuccess()
            } else {
              onMasterPinClearFailure()
            }
          }, {
            Timber.e(it, "error create pin bus")
          })
    }
  }

  fun populateList(forceRefresh: Boolean, onListPopulateBegin: () -> Unit,
      onEntryAddedToList: (AppEntry) -> Unit, onListPopulated: () -> Unit,
      onListPopulateError: (Throwable) -> Unit) {
    disposeOnStop {
      lockListInteractor.populateList(forceRefresh)
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .doAfterTerminate { onListPopulated() }
          .doOnSubscribe { onListPopulateBegin() }
          .subscribe({ onEntryAddedToList(it) }, {
            Timber.e(it, "populateList onError")
            onListPopulateError(it)
          })
    }
  }

  fun setFABStateFromPreference(onSetFABStateEnabled: () -> Unit,
      onSetFABStateDisabled: () -> Unit) {
    disposeOnStop {
      stateInteractor.isServiceEnabled()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (it) {
              onSetFABStateEnabled()
            } else {
              onSetFABStateDisabled()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  fun setSystemVisible() {
    lockListInteractor.setSystemVisible(true)
  }

  fun setSystemInvisible() {
    lockListInteractor.setSystemVisible(false)
  }

  fun setSystemVisibilityFromPreference(onSetSystemVisible: () -> Unit,
      onSetSystemInvisible: () -> Unit) {
    disposeOnStop {
      lockListInteractor.isSystemVisible()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (it) {
              onSetSystemVisible()
            } else {
              onSetSystemInvisible()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  fun showOnBoarding(onOnboardingComplete: () -> Unit, onShowOnboarding: () -> Unit) {
    disposeOnStop {
      lockListInteractor.hasShownOnBoarding()
          .subscribeOn(backgroundScheduler)
          .observeOn(foregroundScheduler)
          .subscribe({
            if (it) {
              onOnboardingComplete()
            } else {
              onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }
}
