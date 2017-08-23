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

import com.pyamsoft.padlock.list.LockListPresenter.Callback
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.pin.ClearPinEvent
import com.pyamsoft.padlock.pin.CreatePinEvent
import com.pyamsoft.padlock.service.LockServiceStateInteractor
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockListPresenter @Inject internal constructor(
    private val lockListInteractor: LockListInteractor,
    private val stateInteractor: LockServiceStateInteractor,
    private val clearPinBus: EventBus<ClearPinEvent>,
    private val createPinBus: EventBus<CreatePinEvent>,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<Callback>(compScheduler, ioScheduler,
    mainScheduler) {

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    registerOnBus(bound::onMasterPinCreateSuccess, bound::onMasterPinCreateFailure,
        bound::onMasterPinClearSuccess, bound::onMasterPinClearFailure)
    setFABStateFromPreference(bound::onSetFABStateEnabled, bound::onSetFABStateDisabled)
    populateList(false, bound::onListPopulateBegin, bound::onEntryAddedToList,
        bound::onListPopulated, bound::onListPopulateError)
  }

  private fun registerOnBus(onMasterPinCreateSuccess: () -> Unit,
      onMasterPinCreateFailure: () -> Unit,
      onMasterPinClearSuccess: () -> Unit, onMasterPinClearFailure: () -> Unit) {
    disposeOnStop {
      createPinBus.listen().subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({
            if (it.success) {
              onMasterPinCreateSuccess()
            } else {
              onMasterPinCreateFailure()
            }
          }, {
            Timber.e(it, "error create pin bus")
          })
    }

    disposeOnStop {
      clearPinBus.listen().subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({
            if (it.success) {
              onMasterPinClearSuccess()
            } else {
              onMasterPinClearFailure()
            }
          }, {
            Timber.e(it, "error create pin bus")
          })
    }
  }

  private fun setFABStateFromPreference(onSetFABStateEnabled: () -> Unit,
      onSetFABStateDisabled: () -> Unit) {
    disposeOnStop {
      stateInteractor.isServiceEnabled()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onSetFABStateEnabled()
            } else {
              onSetFABStateDisabled()
            }
          }, { Timber.e(it, "onError") })
    }
  }

  fun setSystemVisibilityFromPreference(onSetSystemVisibility: (Boolean) -> Unit) {
    disposeOnStop {
      lockListInteractor.isSystemVisible()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            onSetSystemVisibility(it)
          }, { Timber.e(it, "onError") })
    }
  }

  fun setSystemVisibility(visible: Boolean) {
    lockListInteractor.setSystemVisible(visible)
  }

  fun showOnBoarding(onOnboardingComplete: () -> Unit, onShowOnboarding: () -> Unit) {
    disposeOnStop {
      lockListInteractor.hasShownOnBoarding()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            if (it) {
              onOnboardingComplete()
            } else {
              onShowOnboarding()
            }
          }, { Timber.e(it, "onError") })
    }
  }


  fun populateList(force: Boolean, onListPopulateBegin: () -> Unit,
      onEntryAddedToList: (AppEntry) -> Unit, onListPopulated: () -> Unit,
      onListPopulateError: (Throwable) -> Unit) {
    disposeOnStop {
      lockListInteractor.populateList(force)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .doAfterTerminate { onListPopulated() }
          .doOnSubscribe { onListPopulateBegin() }
          .subscribe({ onEntryAddedToList(it) }, {
            Timber.e(it, "populateList onError")
            onListPopulateError(it)
          })
    }
  }

  interface Callback {

    fun onMasterPinCreateSuccess()
    fun onMasterPinCreateFailure()
    fun onMasterPinClearSuccess()
    fun onMasterPinClearFailure()

    fun onListPopulateBegin()
    fun onEntryAddedToList(entry: AppEntry)
    fun onListPopulated()
    fun onListPopulateError(throwable: Throwable)

    fun onSetFABStateEnabled()
    fun onSetFABStateDisabled()

  }
}
