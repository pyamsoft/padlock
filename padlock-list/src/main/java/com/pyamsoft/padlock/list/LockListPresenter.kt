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

import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.list.LockListEvent.Callback.Created
import com.pyamsoft.padlock.list.LockListEvent.Callback.Deleted
import com.pyamsoft.padlock.list.LockListPresenter.BusCallback
import com.pyamsoft.padlock.list.LockListPresenter.Callback
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.padlock.model.LockState
import com.pyamsoft.padlock.model.LockState.DEFAULT
import com.pyamsoft.padlock.model.LockState.LOCKED
import com.pyamsoft.padlock.pin.ClearPinEvent
import com.pyamsoft.padlock.pin.CreatePinEvent
import com.pyamsoft.padlock.service.LockServiceStateInteractor
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockListPresenter @Inject internal constructor(
    private val lockListInteractor: LockListInteractor,
    @Named("cache_lock_list") private val cache: Cache,
    private val stateInteractor: LockServiceStateInteractor,
    private val lockListBus: EventBus<LockListEvent>,
    private val clearPinBus: EventBus<ClearPinEvent>,
    private val createPinBus: EventBus<CreatePinEvent>,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<BusCallback, Callback>(compScheduler,
    ioScheduler,
    mainScheduler) {

  override fun onCreate(bound: BusCallback) {
    super.onCreate(bound)
    registerOnCreateBus(bound::onMasterPinCreateSuccess, bound::onMasterPinCreateFailure)
    registerOnClearBus(bound::onMasterPinClearSuccess, bound::onMasterPinClearFailure)
    registerOnModifyBus(bound::onEntryCreated, bound::onEntryDeleted, bound::onEntryError)
  }

  override fun onStart(bound: Callback) {
    super.onStart(bound)
    setFABStateFromPreference(bound::onSetFABStateEnabled, bound::onSetFABStateDisabled)
    populateList(false, bound::onListPopulateBegin, bound::onEntryAddedToList,
        bound::onListPopulated, bound::onListPopulateError)
  }

  private fun registerOnModifyBus(onEntryCreated: (String) -> Unit,
      onEntryDeleted: (String) -> Unit,
      onEntryError: (Throwable) -> Unit) {
    disposeOnDestroy {
      lockListBus.listen()
          .filter { it is LockListEvent.Modify }
          .map { it as LockListEvent.Modify }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({ modifyDatabaseEntry(it.isChecked, it.packageName, it.code, it.isSystem) }, {
            Timber.e(it, "Error listening to lock list bus")
          })
    }

    disposeOnDestroy {
      lockListBus.listen()
          .filter { it is LockListEvent.Callback }
          .map { it as LockListEvent.Callback }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              is Created -> onEntryCreated(it.packageName)
              is Deleted -> onEntryDeleted(it.packageName)
            }
          }, {
            Timber.e(it, "Error listening to lock info bus")
            onEntryError(it)
          })
    }
  }

  private fun registerOnClearBus(onMasterPinClearSuccess: () -> Unit,
      onMasterPinClearFailure: () -> Unit) {
    disposeOnDestroy {
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

  private fun registerOnCreateBus(onMasterPinCreateSuccess: () -> Unit,
      onMasterPinCreateFailure: () -> Unit) {
    disposeOnDestroy {
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
  }

  private fun modifyDatabaseEntry(isChecked: Boolean, packageName: String, code: String?,
      system: Boolean) {
    disposeOnDestroy {
      // No whitelisting for modifications from the List
      val oldState: LockState
      val newState: LockState
      if (isChecked) {
        oldState = DEFAULT
        newState = LOCKED
      } else {
        oldState = LOCKED
        newState = DEFAULT
      }

      lockListInteractor.modifySingleDatabaseEntry(oldState, newState, packageName,
          PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system)
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            when (it) {
              LockState.DEFAULT -> lockListBus.publish(LockListEvent.Callback.Deleted(packageName))
              LockState.LOCKED -> lockListBus.publish(LockListEvent.Callback.Created(packageName))
              else -> throw RuntimeException("Whitelist/None results are not handled")
            }
          }, {
            Timber.e(it, "onError modifyDatabaseEntry")
            lockListBus.publish(LockListEvent.Callback.Error(it))
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

  /**
   * Used when the activity is launched from Notification
   */
  fun forceClearCache() {
    cache.clearCache()
  }

  interface BusCallback {

    fun onMasterPinCreateSuccess()
    fun onMasterPinCreateFailure()

    fun onMasterPinClearSuccess()
    fun onMasterPinClearFailure()

    fun onEntryCreated(packageName: String)

    fun onEntryDeleted(packageName: String)

    fun onEntryError(throwable: Throwable)

  }

  interface Callback {

    fun onListPopulateBegin()
    fun onEntryAddedToList(entry: AppEntry)
    fun onListPopulated()
    fun onListPopulateError(throwable: Throwable)

    fun onSetFABStateEnabled()
    fun onSetFABStateDisabled()

  }
}
