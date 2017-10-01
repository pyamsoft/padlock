/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.list.LockListEvent.Callback.Created
import com.pyamsoft.padlock.list.LockListEvent.Callback.Deleted
import com.pyamsoft.padlock.list.LockListPresenter.BusCallback
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
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<BusCallback>(compScheduler,
    ioScheduler,
    mainScheduler) {

  override fun onBind(v: BusCallback) {
    super.onBind(v)
    registerOnCreateBus(v::onMasterPinCreateSuccess, v::onMasterPinCreateFailure)
    registerOnClearBus(v::onMasterPinClearSuccess, v::onMasterPinClearFailure)
    registerOnModifyBus(v::onEntryCreated, v::onEntryDeleted, v::onEntryError)
  }

  private fun registerOnModifyBus(onEntryCreated: (String) -> Unit,
      onEntryDeleted: (String) -> Unit,
      onEntryError: (Throwable) -> Unit) {
    dispose {
      lockListBus.listen()
          .filter { it is LockListEvent.Modify }
          .map { it as LockListEvent.Modify }
          .subscribeOn(ioScheduler).observeOn(mainThreadScheduler)
          .subscribe({ modifyDatabaseEntry(it.isChecked, it.packageName, it.code, it.isSystem) }, {
            Timber.e(it, "Error listening to lock list bus")
          })
    }

    dispose {
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
    dispose {
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
    dispose {
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
    dispose {
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

  fun setFABStateFromPreference(onSetFABStateEnabled: () -> Unit,
      onSetFABStateDisabled: () -> Unit) {
    dispose {
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
    dispose {
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
    dispose {
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
    dispose {
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
}
